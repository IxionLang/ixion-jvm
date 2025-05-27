package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.compiler.VariableType;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.parser.Value;

import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AssignmentNode implements Node {

	private final Token op;
	private final Node left;
	private final Node right;
	private boolean isStaticAccess;

	private boolean isExpressionStatementBody = false;

	public AssignmentNode(Node left, Token op, Node right) {
		this.op = op;
		this.left = left;
		this.right = right;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		Value valueType = left.getLValue();

		if(valueType == Value.NONE) {
			throw new IxException(op, "Invalid lvalue - cannot assign");
		}

		IxType returnType = right.getReturnType(context.getContext());

		context.getContext().updateLine(op.line());

		switch (valueType) {
			case VARIABLE -> variable(context, returnType);
			case PROPERTY -> property(context, returnType);
			case ARRAY -> array(context, returnType);
			case NULLABLE_PROPERTY -> nullableProperty(context, returnType);
			case NULLABLE_ARRAY -> nullableArray(context, returnType);
		}
	}

	private void variable(FileContext context, IxType returnType) throws IxException {
		Object[] lValueData = left.getLValueData();
		Token name = (Token) lValueData[0];

		Variable variable = context.getContext().getScope().lookupVariable(name.value());

		if(variable == null) {
			throw new IxException(op, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
		}

		if(variable.isConst()) {
			throw new IxException(name, "Reassignment of constant '%s'.".formatted(name.value()));
		}

		if(variable.getVariableType() == VariableType.CLASS) {
			if(context.getContext().isStaticMethod())  throw new IxException(name, "Cannot access instance member '%s' in a static context".formatted(name.value()));
			context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
		}

		generateSyntheticOperation().visit(context);

		try {
			if(!variable.getType().isAssignableFrom(returnType, context.getContext(), true)) {
				throw new IxException(op,
						"Cannot assign type '%s' to variable of type '%s'"
								.formatted(returnType, variable.getType()));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();
		if(!isExpressionStatementBody) methodVisitor.visitInsn(returnType.getDupOpcode());

		if(variable.getVariableType() == VariableType.STATIC) {
			methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
		}
		if(variable.getVariableType() == VariableType.CLASS) {
			methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
		}
		else {
			methodVisitor.visitVarInsn(variable.getType().getOpcode(Opcodes.ISTORE), variable.getIndex());
		}
	}

	private void property(FileContext context, IxType returnType) throws IxException {
		Object[] lValueData = left.getLValueData();

		Node obj = (Node) lValueData[0];
		Token name = (Token) lValueData[1];

		obj.visit(context);

		generateSyntheticOperation().visit(context);

		IxType objType = getObjectType(obj, context.getContext());

		if(!objType.isObject()) {
			throw new IxException(name, "Cannot access member on type '%s'".formatted(objType));
		}

		if(objType.isNullable()) {
			throw new IxException(name, "Cannot use '.' to access members on a nullable type ('%s')".formatted(objType));
		}

		handlePropertySettingLogic(obj, objType, name, returnType, context, op, isStaticAccess, isExpressionStatementBody);
	}

	private void array(FileContext context, IxType returnType) throws IxException {
		Object[] lValueData = left.getLValueData();

		Node array = (Node) lValueData[0];
		Node index = (Node) lValueData[1];
		Token bracket = (Token) lValueData[2];

		IxType arrayType = array.getReturnType(context.getContext());
		IxType indexType = index.getReturnType(context.getContext());

		if(!arrayType.isArray()) {
			throw new IxException(op, "Cannot get index of type '%s'".formatted(arrayType));
		}
		if(arrayType.isNullable()) {
			throw new IxException(bracket, "Cannot use '[' to access members on a nullable type ('%s')".formatted(arrayType));
		}

		if(!indexType.isRepresentedAsInteger()) {
			throw new IxException(op, "Index must be an integer type (got '%s')".formatted(indexType));
		}

		array.visit(context);
		index.visit(context);

		try {
			if(!arrayType.getElementType().isAssignableFrom(returnType, context.getContext(), true)) {
				throw new IxException(op,
						"Cannot assign type '%s' to element of type '%s'"
								.formatted(returnType, arrayType.getElementType()));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		generateSyntheticOperation().visit(context);

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		if(!isExpressionStatementBody) methodVisitor.visitInsn(returnType.getDupX2Opcode());

		methodVisitor.visitInsn(arrayType.getElementType().getOpcode(Opcodes.IASTORE));
	}

	private void nullableProperty(FileContext context, IxType returnType) throws IxException {
		Object[] lValueData = left.getLValueData();

		Node obj = (Node) lValueData[0];
		Token name = (Token) lValueData[1];

		IxType objType = getObjectType(obj, context.getContext());

		if(!objType.isObject()) {
			throw new IxException(name, "Cannot access member on type '%s'".formatted(objType));
		}

		if(!objType.isNullable()) {
			throw new IxException(name, "Cannot use '?.' to access members on a non-nullable type ('%s')".formatted(objType));
		}

		Label nullJump = new Label();
		Label end = new Label();

		context.getContext().setNullJumpLabel(nullJump);

		obj.visit(context);

		context.getContext().setNullJumpLabel(null);

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		methodVisitor.visitInsn(Opcodes.DUP);
		methodVisitor.visitJumpInsn(Opcodes.IFNULL, nullJump);

		generateSyntheticOperation().visit(context);

		handlePropertySettingLogic(obj, objType, name, returnType, context, op, isStaticAccess, isExpressionStatementBody);

		methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

		methodVisitor.visitLabel(nullJump);
		methodVisitor.visitInsn(Opcodes.POP);
		methodVisitor.visitLabel(end);
	}

	private void nullableArray(FileContext context, IxType returnType) throws IxException {
		Object[] lValueData = left.getLValueData();

		Node array = (Node) lValueData[0];
		Node index = (Node) lValueData[1];
		Token bracket = (Token) lValueData[2];

		IxType arrayType = array.getReturnType(context.getContext());
		IxType indexType = index.getReturnType(context.getContext());

		if(!arrayType.isArray()) {
			throw new IxException(op, "Cannot get index of type '%s'".formatted(arrayType));
		}
		if(!arrayType.isNullable()) {
			throw new IxException(bracket, "Cannot use '?[' to access members on a non-nullable type ('%s')".formatted(arrayType));
		}

		if(!indexType.isInteger()) {
			throw new IxException(op, "Index must be an integer type (got '%s')".formatted(indexType));
		}

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();
		Label nullJump = new Label();
		Label end = new Label();

		context.getContext().setNullJumpLabel(nullJump);

		array.visit(context);

		context.getContext().setNullJumpLabel(null);

		methodVisitor.visitInsn(arrayType.getDupOpcode());
		methodVisitor.visitJumpInsn(Opcodes.IFNULL, nullJump);

		index.visit(context);

		try {
			if(!arrayType.getElementType().isAssignableFrom(returnType, context.getContext(), true)) {
				throw new IxException(op,
						"Cannot assign type '%s' to element of type '%s'"
								.formatted(returnType, arrayType.getElementType()));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		generateSyntheticOperation().visit(context);


		if(!isExpressionStatementBody) methodVisitor.visitInsn(returnType.getDupX2Opcode());

		methodVisitor.visitInsn(arrayType.getElementType().getOpcode(Opcodes.IASTORE));

		methodVisitor.visitJumpInsn(Opcodes.GOTO, end);

		methodVisitor.visitLabel(nullJump);
		methodVisitor.visitInsn(Opcodes.POP);
		methodVisitor.visitLabel(end);

	}

	public static void handlePropertySettingLogic(Node obj, IxType objType, Token name, IxType returnType, FileContext context,
                                                  Token op, boolean isStaticAccess, boolean isExpressionStatementBody) throws IxException {
		Class<?> klass;

		try {
			klass = Class.forName(objType.getClassName(), false, context.getContext().getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(name, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		if(!isExpressionStatementBody) context.getContext().getMethodVisitor().visitInsn(returnType.getDupX1Opcode());

		try {
			Field f = klass.getDeclaredField(name.value());

			try {
				if(!IxType.getType(f).isAssignableFrom(returnType, context.getContext(), true)) {
					throw new IxException(op,
							"Cannot assign type '%s' to variable of type '%s'"
									.formatted(returnType, IxType.getType(f.getType())));
				}
			} catch (ClassNotFoundException e) {
				throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
			}

			if(!Modifier.isPublic(f.getModifiers()) && !objType.equals(IxType.getObjectType(context.getContext().getCurrentClass()))) {
				throw new NoSuchFieldException();
			}

			if(!isStaticAccess && Modifier.isStatic(f.getModifiers())) {
				throw new IxException(name, "Cannot access static member from non-static object.");
			}

			if(Modifier.isFinal(f.getModifiers())) {
				if (!(obj instanceof ThisNode) || !context.getContext().isConstructor())
					throw new IxException(name, "Cannot assign final member '%s'".formatted(name.value()));
			}

			context.getContext().getMethodVisitor().visitFieldInsn(TypeUtil.getMemberPutOpcode(f),
					objType.getInternalName(), name.value(), Type.getType(f.getType()).getDescriptor());

		} catch (NoSuchFieldException e) {
			String base = name.value().substring(0, 1).toUpperCase() + name.value().substring(1);
			String setName = "set" + (name.value().matches("^is[\\p{Lu}].*") ? base.substring(2) : base);

			Method m = resolveSetMethod(klass, name, setName, returnType, context.getContext(), isStaticAccess);

			TypeUtil.insertInvoke(context, m, false);
		}
	}

	private static Method resolveSetMethod(Class<?> klass, Token location, String name, IxType arg, Context context, boolean isStaticAccess) throws IxException {
		ArrayList<Pair<Integer, Method>> possible = new ArrayList<>();

		try {
			for (Method method : klass.getMethods()) {
				if(!method.getName().equals(name)) continue;
				IxType[] expectArgs = IxType.getType(method).getArgumentTypes();

				if (expectArgs.length != 1) continue;

				int changes = 0;

				IxType expectArg = expectArgs[0];

				if (arg.equals(IxType.VOID_TYPE))
					continue;

				if (expectArg.isAssignableFrom(arg, context, false)) {
					if (!expectArg.equals(arg)) changes += expectArg.assignChangesFrom(arg);
				} else {
					continue;
				}
				possible.add(new Pair<>(changes, method));
			}
		}
		catch(ClassNotFoundException e) {
			throw new IxException(location, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		if(possible.isEmpty()) {
			throw new IxException(location,
					"Could not resolve field '%s' in class '%s' with type '%s'".formatted(name, klass.getName(),
							arg));
		}

		List<Pair<Integer, Method>> appliedPossible = possible.stream()
				.filter(p -> Modifier.isStatic(p.second().getModifiers()) == isStaticAccess)
				.sorted(Comparator.comparingInt(Pair::first)).toList();

		if(appliedPossible.isEmpty()) {
			if(isStaticAccess)
				// Shouldn't be thrown
				throw new IxException(location, "Cannot invoke non-static member from static class.");
			else
				throw new IxException(location, "Cannot access static member from non-static object.");
		}

		return appliedPossible.getFirst().second();
	}

	private Token makeSyntheticToken() {
		return new Token(switch(op.type()) {
			case IN_PLUS -> TokenType.PLUS;
			case IN_MINUS -> TokenType.MINUS;
			case IN_MUL -> TokenType.STAR;
			case IN_DIV -> TokenType.SLASH;
			case IN_MOD -> TokenType.PERCENT;
			case IN_BITWISE_AND -> TokenType.BITWISE_AND;
			case IN_BITWISE_OR -> TokenType.BITWISE_OR;
			case IN_BITWISE_XOR -> TokenType.BITWISE_XOR;
			case IN_BITWISE_SHL -> TokenType.BITWISE_SHL;
			case IN_BITWISE_SHR -> TokenType.BITWISE_SHR;
			case IN_BITWISE_USHR -> TokenType.BITWISE_USHR;
			default -> null;
		}, op.value(), op.line(), op.column());
	}

	private boolean isBitwise(TokenType type) {
		return switch (type) {
			case IN_BITWISE_AND, IN_BITWISE_OR, IN_BITWISE_XOR, IN_BITWISE_SHL, IN_BITWISE_SHR, IN_BITWISE_USHR -> true;
			default -> false;
		};
	}

	private Node generateSyntheticOperation() {
		if(op.type() == TokenType.EQUALS) return right;

		Token syntheticOp = makeSyntheticToken();

		if(isBitwise(op.type())) {
			return new IntegerOperationNode(left, syntheticOp, right);
		}

		return new ArithmeticOperationNode(left, syntheticOp, right);
	}

	private IxType getObjectType(Node obj, Context context) throws IxException {
		if(obj instanceof VariableAccessNode) {
			VariableAccessNode van = (VariableAccessNode) obj;
			van.setMemberAccess(true);
			IxType leftType = obj.getReturnType(context);
			isStaticAccess = van.isStaticClassAccess();
			return leftType;
		}
		return obj.getReturnType(context);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return right.getReturnType(context);
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(left, op.value(), right);
	}

	public void setExpressionStatementBody(boolean expressionStatementBody) {
		isExpressionStatementBody = expressionStatementBody;
	}
}
