package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.compiler.VariableType;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class UpdateExpressionNode implements Node {

	private final Node expression;
	private final Token operation;
	private final boolean prefix;
	private final boolean increment;
	private boolean isStaticAccess;

	public UpdateExpressionNode(Node expression, Token operation, boolean prefix) {
		this.expression = expression;
		this.operation = operation;
		this.prefix = prefix;
		this.increment = operation.type() == TokenType.INCREMENT;
		this.isStaticAccess = false;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		if(expression.getLValue() == Value.NONE) {
			throw new IxException(operation, "Invalid lvalue - cannot perform operation");
		}

		switch (expression.getLValue()) {
			case VARIABLE -> variable(context);
			case ARRAY -> array(context);
			case PROPERTY -> property(context);
			default -> throw new IxException(operation, "Invalid lvalue - cannot perform operation");
		}

	}

	private void variable(FileContext context) throws IxException {
		Object[] lValueData = expression.getLValueData();
		Token name = (Token) lValueData[0];

		Variable variable = context.getContext().getScope().lookupVariable(name.value());

		if(variable == null) {
			throw new IxException(operation, "Cannot resolve variable '%s' in current scope.".formatted(name.value()));
		}

		if(variable.isConst()) {
			throw new IxException(operation, "Reassignment of constant '%s'.".formatted(name.value()));
		}

		if(variable.getVariableType() == VariableType.CLASS) {
			if (context.getContext().isStaticMethod())
				throw new IxException(name, "Cannot access instance member '%s' in a static context".formatted(name.value()));
		}

		if(!variable.getType().isNumeric()) {
			throw new IxException(operation, "Update expression ('%s') target must be numeric (got '%s')".formatted(operation.value(),
					variable.getType()));
		}

		IxType type = variable.getType();

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		if(variable.getVariableType() == VariableType.LOCAL && type.getSort() == IxType.Sort.INT) {
			int value = increment ? 1 : -1;

			if(!prefix) visitor.visitVarInsn(Opcodes.ILOAD, variable.getIndex());

			visitor.visitIincInsn(variable.getIndex(), value);

			if(prefix) visitor.visitVarInsn(Opcodes.ILOAD, variable.getIndex());

			return;
		}

		switch (variable.getVariableType()) {
			case LOCAL ->  {
				visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), variable.getIndex());
				if(!prefix) visitor.visitInsn(type.getDupOpcode());
			}
			case STATIC ->  {
				visitor.visitFieldInsn(Opcodes.GETSTATIC, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
				if(!prefix) visitor.visitInsn(type.getDupOpcode());
			}
			case CLASS -> {
				visitor.visitVarInsn(Opcodes.ALOAD, 0);
				visitor.visitInsn(Opcodes.DUP);
				visitor.visitFieldInsn(Opcodes.GETFIELD, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
				if(!prefix) visitor.visitInsn(type.getDupX1Opcode());
			}
		}

		type.generateAsInteger(1, context.getContext());

		if(increment) visitor.visitInsn(type.getOpcode(Opcodes.IADD));
		else visitor.visitInsn(type.getOpcode(Opcodes.ISUB));

		switch (variable.getVariableType()) {
			case LOCAL -> {
				if(prefix) visitor.visitInsn(type.getDupOpcode());
				visitor.visitVarInsn(type.getOpcode(Opcodes.ISTORE), variable.getIndex());
			}
			case STATIC ->  {
				if(prefix) visitor.visitInsn(type.getDupOpcode());
				visitor.visitFieldInsn(Opcodes.PUTSTATIC, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
			}
			case CLASS -> {
				if(prefix) visitor.visitInsn(type.getDupX1Opcode());
				visitor.visitFieldInsn(Opcodes.PUTFIELD, variable.getOwner(), variable.getName(), variable.getType().getDescriptor());
			}
		}

	}

	private void array(FileContext context) throws IxException {
		Object[] lValueData = expression.getLValueData();

		Node array = (Node) lValueData[0];
		Node index = (Node) lValueData[1];
		Token bracket = (Token) lValueData[2];

		IxType arrayType = array.getReturnType(context.getContext());
		IxType indexType = index.getReturnType(context.getContext());

		if(!arrayType.isArray()) {
			throw new IxException(bracket, "Cannot get index of type '%s'".formatted(arrayType));
		}
		if(arrayType.isNullable()) {
			throw new IxException(bracket, "Cannot use '[' to access members on a nullable type ('%s')".formatted(arrayType));
		}

		if(!indexType.isRepresentedAsInteger()) {
			throw new IxException(bracket, "Index must be an integer type (got '%s')".formatted(indexType));
		}

		IxType elementType = arrayType.getElementType();

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		if(!elementType.isNumeric()) {
			throw new IxException(operation, "Update expression ('%s') target must be numeric (got '%s')".formatted(operation.value(), elementType));
		}

		array.visit(context);
		index.visit(context);

		visitor.visitInsn(Opcodes.DUP2);

		visitor.visitInsn(elementType.getOpcode(Opcodes.IALOAD));

		if(!prefix) visitor.visitInsn(elementType.getDupX2Opcode());

		elementType.generateAsInteger(1, context.getContext());

		if(increment) visitor.visitInsn(elementType.getOpcode(Opcodes.IADD));
		else visitor.visitInsn(elementType.getOpcode(Opcodes.ISUB));

		if(prefix) visitor.visitInsn(elementType.getDupX2Opcode());

		visitor.visitInsn(elementType.getOpcode(Opcodes.IASTORE));
	}

	private void property(FileContext context) throws IxException {
		Object[] lValueData = expression.getLValueData();

		Node obj = (Node) lValueData[0];
		Token name = (Token) lValueData[1];

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		obj.visit(context);

		IxType objType = getObjectType(obj, context.getContext());

		if(!isStaticAccess) visitor.visitInsn(Opcodes.DUP);

		if(!objType.isObject()) {
			throw new IxException(name, "Cannot access member on type '%s'".formatted(objType));
		}

		if(objType.isNullable()) {
			throw new IxException(name, "Cannot use '.' to access members on a nullable type ('%s')".formatted(objType));
		}

		MemberAccessNode syntheticAccess = new MemberAccessNode(obj, name);

		IxType type = syntheticAccess.getReturnType(context.getContext());
		if(!type.isNumeric()) {
			throw new IxException(operation, "Update expression ('%s') target must be numeric (got '%s')".formatted(operation.value(), type));
		}

		syntheticAccess.visitAccess(context);

		if(!prefix) {
			if(!isStaticAccess) {
				visitor.visitInsn(Opcodes.DUP_X1);
				visitor.visitInsn(type.getDupX1Opcode());
			}
			else {
				visitor.visitInsn(type.getDupOpcode());
			}
		}

		type.generateAsInteger(1, context.getContext());

		if(increment) visitor.visitInsn(type.getOpcode(Opcodes.IADD));
		else visitor.visitInsn(type.getOpcode(Opcodes.ISUB));

		if(prefix && isStaticAccess) {
			visitor.visitInsn(Opcodes.DUP);
		}

		AssignmentNode.handlePropertySettingLogic(obj, objType, name, type, context, operation, isStaticAccess, !(prefix && !isStaticAccess));

		if(!prefix && !isStaticAccess) {
			type.swap(objType, context.getContext().getMethodVisitor());
			visitor.visitInsn(Opcodes.POP);
		}
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
		IxType returnType = expression.getReturnType(context);
		if(!returnType.isNumeric()) {
			throw new IxException(operation, "Update expression ('%s') target must be numeric (got '%s')".formatted(operation.value(), returnType));
		}
		return returnType;
	}

	@Override
	public String toString() {
		return prefix ? operation.value() + expression : expression + operation.value();
	}
}
