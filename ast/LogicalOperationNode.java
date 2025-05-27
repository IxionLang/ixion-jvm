package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LogicalOperationNode implements Node {

	private final Node left;
	private final Token op;
	private final Node right;

	public LogicalOperationNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		if(!verifyTypes(leftType, rightType)) {
			throw new IxException(op, "Unsupported operation of '%s' between types '%s' and '%s'".formatted(
					op.value(),
					leftType,
					rightType
			));
		}

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		if(op.type() == TokenType.LOGICAL_AND) {
			Label falseL = new Label();
			Label end = new Label();

			left.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);

			right.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);

			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitJumpInsn(Opcodes.GOTO, end);
			visitor.visitLabel(falseL);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitLabel(end);
		}
		else if(op.type() == TokenType.LOGICAL_OR) {
			Label trueL = new Label();
			Label falseL = new Label();
			Label end = new Label();

			left.visit(context);

			visitor.visitJumpInsn(Opcodes.IFNE, trueL);

			right.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);

			visitor.visitLabel(trueL);
			visitor.visitInsn(Opcodes.ICONST_1);
			visitor.visitJumpInsn(Opcodes.GOTO, end);

			visitor.visitLabel(falseL);
			visitor.visitInsn(Opcodes.ICONST_0);
			visitor.visitLabel(end);
		}
	}

	public boolean generateConditional(FileContext context, Label falseL) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		if(!verifyTypes(leftType, rightType)) {
			throw new IxException(op, "Unsupported operation of '%s' between types '%s' and '%s'".formatted(
					op.value(),
					leftType,
					rightType
			));
		}

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		if(op.type() == TokenType.LOGICAL_AND) {
			left.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);

			right.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);
		}
		else if(op.type() == TokenType.LOGICAL_OR) {
			Label end = new Label();
			left.visit(context);

			visitor.visitJumpInsn(Opcodes.IFNE, end);

			right.visit(context);

			visitor.visitJumpInsn(Opcodes.IFEQ, falseL);
			visitor.visitLabel(end);
		}
		return true;
	}

	private boolean verifyTypes(IxType left, IxType right) {
		return left.equals(IxType.BOOLEAN_TYPE) && right.equals(IxType.BOOLEAN_TYPE);
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
