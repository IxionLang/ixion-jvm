package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class ArithmeticOperationNode implements Node {

	private final Node left;
	private final Token op;
	private final Node right;

	public ArithmeticOperationNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		if(isConstant(context.getContext()) && resolvableConstantWithOptimizations(leftType, rightType, context)) {
			Object val = getConstantValue(context.getContext());
			TypeUtil.correctLdc(val, context.getContext());
		}

		else if(leftType.isPrimitive() && rightType.isPrimitive()) {
			IxType larger = leftType.getLarger(rightType);

			boolean same = leftType.equals(rightType);

			left.visit(context);

			if(!same && larger.equals(rightType)) {
				leftType.cast(rightType, visitor);
			}

			right.visit(context);

			if(!same && larger.equals(leftType)) {
				rightType.cast(leftType, visitor);
			}

			visitor.visitInsn(larger.getOpcode(getOpcode()));

		}

		else if((leftType.equals(IxType.STRING_TYPE) || rightType.equals(IxType.STRING_TYPE)) && op.type() == TokenType.PLUS) {
			concatStrings(context);
		}
		else if(leftType.equals(IxType.STRING_TYPE) && rightType.isRepresentedAsInteger() && op.type() == TokenType.STAR) {
			left.visit(context);
			right.visit(context);
			context.getContext().getMethodVisitor().visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "repeat", "(I)Ljava/lang/String;", false);
		}
		else if(leftType.isRepresentedAsInteger() && rightType.equals(IxType.STRING_TYPE) && op.type() == TokenType.STAR) {
			left.visit(context);
			right.visit(context);
			rightType.swap(leftType, context.getContext().getMethodVisitor());
			context.getContext().getMethodVisitor().visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "repeat", "(I)Ljava/lang/String;", false);
		}

		else throw new IxException(op, "Unsupported operation of '%s' between types '%s' and '%s'".formatted(op.value(), leftType, rightType));
	}

	private boolean resolvableConstantWithOptimizations(IxType leftType, IxType rightType, FileContext context) {
		return context.shouldOptimize("constant.arithmetic") && leftType.isNumeric() && rightType.isNumeric(); // Numerical arithmetic
	}

	private void concatStrings(FileContext fc) throws IxException {
		Context context = fc.getContext();
		StringBuilder descriptor = new StringBuilder("(");
		StringBuilder recipe = new StringBuilder();

		concatStrings(left, right, descriptor, recipe, fc);

		descriptor.append(")Ljava/lang/String;");

		if(fc.shouldOptimize("constant.string.concat") && recipe.indexOf("\u0001") == -1) {
			context.getMethodVisitor().visitLdcInsn(recipe.toString());
		}
		else {
			context.getMethodVisitor().visitInvokeDynamicInsn("makeConcatWithConstants", descriptor.toString(), new Handle(Opcodes.H_INVOKESTATIC,
					"java/lang/invoke/StringConcatFactory",
					"makeConcatWithConstants",
					"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
					false), recipe.toString());
		}
	}

	private void concatStrings(Node left, Node right, StringBuilder descriptor, StringBuilder recipe, FileContext fc) throws IxException {
		Context context = fc.getContext();
		if(left instanceof ArithmeticOperationNode && ((ArithmeticOperationNode) left).op.type() == TokenType.PLUS) {
			ArithmeticOperationNode operation = (ArithmeticOperationNode) left;
			concatStrings(operation.left, operation.right, descriptor, recipe, fc);
		}

		else if(left.isConstant(context)) {
			recipe.append(left.getConstantValue(context));
		}
		else {
			descriptor.append(left.getReturnType(context).getDescriptor());
			recipe.append('\u0001');
			left.visit(fc);
		}

		if(right.isConstant(context)) {
			recipe.append(right.getConstantValue(context));
		}
		else {
			descriptor.append(right.getReturnType(context).getDescriptor());
			recipe.append('\u0001');
			right.visit(fc);
		}
	}

	private int getOpcode() {
		return switch (op.type()) {
			case PLUS -> Opcodes.IADD;
			case MINUS -> Opcodes.ISUB;
			case STAR -> Opcodes.IMUL;
			case SLASH -> Opcodes.IDIV;
			case PERCENT -> Opcodes.IREM;
			default -> 0;
		};
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType leftType = left.getReturnType(context);
		IxType rightType = right.getReturnType(context);

		if(leftType.equals(IxType.STRING_TYPE) || rightType.equals(IxType.STRING_TYPE))
			return IxType.STRING_TYPE;

		return leftType.getLarger(rightType);
	}

	@Override
	public Object getConstantValue(Context context) throws IxException {
		Object leftVal = left.getConstantValue(context);
		Object rightVal = right.getConstantValue(context);

		if((leftVal.getClass().equals(String.class) || rightVal.getClass().equals(String.class)) && op.type() == TokenType.PLUS) {
			return leftVal.toString() + rightVal.toString();
		}

		if(leftVal.getClass().equals(Integer.class) && rightVal.getClass().equals(Integer.class)) {
			return intOp((int) leftVal, (int) rightVal);
		}
		double dLeft = (double) (leftVal.getClass().equals(Integer.class) ? ((Integer) leftVal).doubleValue() : leftVal);
		double dRight = (double) (rightVal.getClass().equals(Integer.class) ? ((Integer) rightVal).doubleValue() : rightVal);

		return doubleOp(dLeft, dRight);
	}

	private int intOp(int left, int right) {
		return switch (op.type()) {
			case PLUS -> left + right;
			case MINUS -> left - right;
			case STAR -> left * right;
			case SLASH -> left / right;
			case PERCENT -> left % right;
			default -> 0;
		};
	}

	private double doubleOp(double left, double right) {
		return switch (op.type()) {
			case PLUS -> left + right;
			case MINUS -> left - right;
			case STAR -> left * right;
			case SLASH -> left / right;
			case PERCENT -> left % right;
			default -> 0;
		};
	}

	@Override
	public boolean isConstant(Context context) throws IxException {
		List<Class<?>> shouldBeConstant = List.of(String.class, Integer.class, Double.class);
		IxType leftReturnType = left.getReturnType(context);
		IxType rightReturnType = right.getReturnType(context);

		try {
			if (!shouldBeConstant.contains(leftReturnType.toClass(context)) || !shouldBeConstant.contains(rightReturnType.toClass(context)))
				return false;
		}
		catch (ClassNotFoundException e) {
			throw new IxException(op, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		if((leftReturnType.equals(IxType.STRING_TYPE) || rightReturnType.equals(IxType.STRING_TYPE)) && op.type() == TokenType.STAR) return false;
		return left.isConstant(context) && right.isConstant(context);
	}

	@Override
	public String toString() {
		String leftStr = left instanceof ArithmeticOperationNode ? "(" + left.toString() + ")" : left.toString();
		String rightStr = right instanceof ArithmeticOperationNode ? "(" + right.toString() + ")" : right.toString();

		return "%s %s %s".formatted(leftStr, op.value(), rightStr);
	}
}
