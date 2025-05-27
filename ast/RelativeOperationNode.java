package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RelativeOperationNode implements Node {
	private final Node left;
	private final Token op;
	private final Node right;

	public RelativeOperationNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
		// Assert against incorrect token type, as fault of the parser.
		assert op.type() == TokenType.LESS || op.type() == TokenType.LESS_EQ || op.type() == TokenType.GREATER || op.type() == TokenType.GREATER_EQ;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		Label falseL = new Label();
		Label end = new Label();
		generateConditional(context, falseL);

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		methodVisitor.visitInsn(Opcodes.ICONST_1);
		methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
		methodVisitor.visitLabel(falseL);
		methodVisitor.visitInsn(Opcodes.ICONST_0);
		methodVisitor.visitLabel(end);
	}

	public void generateConditional(FileContext context, Label falseL) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		if(leftType.isPrimitive() && rightType.isPrimitive()) {

			if(!leftType.isNumeric() || !rightType.isNumeric())
				throw new IxException(op,
						"Unsupported operation of '%s' between types '%s' and '%s'".formatted(op.value(),
								leftType, rightType));

			boolean same = leftType.equals(rightType);

			IxType larger = leftType.getLarger(rightType);

			left.visit(context);

			if(!same && larger.equals(rightType)) {
				leftType.cast(rightType, methodVisitor);
			}

			right.visit(context);

			if(!same && larger.equals(leftType)) {
				rightType.cast(leftType, methodVisitor);
			}

			if(larger.isRepresentedAsInteger()) {
				switch (op.type()) {
					case LESS -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPGE, falseL);
					case LESS_EQ -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPGT, falseL);
					case GREATER -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPLE, falseL);
					case GREATER_EQ -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPLT, falseL);
				}
			}
			else {
				larger.compareInit(methodVisitor);

				switch (op.type()) {
					case LESS -> generateIfBytecode(methodVisitor, Opcodes.IFGE, falseL);
					case LESS_EQ -> generateIfBytecode(methodVisitor, Opcodes.IFGT, falseL);
					case GREATER -> generateIfBytecode(methodVisitor, Opcodes.IFLE, falseL);
					case GREATER_EQ -> generateIfBytecode(methodVisitor, Opcodes.IFLT, falseL);
				}
			}

		}
		else
			throw new IxException(op,
					"Unsupported operation of '%s' between types '%s' and '%s'".formatted(op.value(),
							leftType, rightType));
	}

	private void generateIfBytecode(MethodVisitor methodVisitor, int opcode, Label falseL) {
		methodVisitor.visitJumpInsn(opcode, falseL);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.BOOLEAN_TYPE;
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(left, op.value(), right);
	}
}
