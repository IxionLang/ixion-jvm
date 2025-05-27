package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EqualityOperationNode implements Node {

	private final Node left;
	private final Token op;
	private final Node right;

	public EqualityOperationNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
		assert op.type() == TokenType.EQEQ || op.type() == TokenType.EXEQ || op.type() == TokenType.TRI_EQ || op.type() == TokenType.TRI_EXEQ;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		Label falseL = new Label();
		Label end = new Label();

		if(generateConditional(context, falseL)) {
			MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

			methodVisitor.visitInsn(Opcodes.ICONST_1);
			methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
			methodVisitor.visitLabel(falseL);
			methodVisitor.visitInsn(Opcodes.ICONST_0);
			methodVisitor.visitLabel(end);
		}
	}

	public boolean generateConditional(FileContext context, Label falseL) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		if(leftType.isPrimitive() && rightType.isPrimitive()) {
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
					case EQEQ -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPNE, falseL);
					case EXEQ -> generateIfBytecode(methodVisitor, Opcodes.IF_ICMPEQ, falseL);
					case TRI_EQ, TRI_EXEQ -> throw new IxException(op,
							"Cannot perform address comparison on primitives ('%s', '%s')"
									.formatted(leftType, rightType));
				}
			}
			else {
				larger.compareInit(methodVisitor);

				switch (op.type()) {
					case EQEQ -> generateIfBytecode(methodVisitor, Opcodes.IFNE, falseL);
					case EXEQ -> generateIfBytecode(methodVisitor, Opcodes.IFEQ, falseL);
					case TRI_EQ, TRI_EXEQ -> throw new IxException(op,
							"Cannot perform address comparison on primitives ('%s', '%s')"
									.formatted(leftType, rightType));
				}
			}
			return true;
		}
		else if(!leftType.isPrimitive() && !rightType.isPrimitive()) {
			left.visit(context);

			if(!(rightType.isNull() && (op.type() == TokenType.TRI_EQ || op.type() == TokenType.TRI_EXEQ))) right.visit(context);
			switch (op.type()) {
				case EQEQ -> {
					isEqual(methodVisitor, leftType);
					return false;
				}
				case EXEQ -> {
					isEqual(methodVisitor, leftType);
					not(methodVisitor, falseL);
					return true;
				}
				case TRI_EQ -> {
					if(rightType.isNull()) generateIfBytecode(methodVisitor, Opcodes.IFNONNULL, falseL);
					else generateIfBytecode(methodVisitor, Opcodes.IF_ACMPNE, falseL);
					return true;
				}
				case TRI_EXEQ -> {
					if(rightType.isNull()) generateIfBytecode(methodVisitor, Opcodes.IFNULL, falseL);
					else generateIfBytecode(methodVisitor, Opcodes.IF_ACMPEQ, falseL);
					return true;
				}
			}
		}
		else {

			left.visit(context);
			leftType = leftType.autoBox(methodVisitor);

			right.visit(context);
			rightType.autoBox(methodVisitor);

			switch (op.type()) {
				case EQEQ -> isEqual(methodVisitor, leftType);
				case EXEQ -> {
					isEqual(methodVisitor, leftType);
					not(methodVisitor, falseL);
				}
				case TRI_EQ, TRI_EXEQ -> throw new IxException(op,
						"Cannot perform address comparison on types ('%s', '%s')"
								.formatted(leftType, rightType));
			}
		}
		return false;
	}

	private void isEqual(MethodVisitor methodVisitor, IxType owner) throws IxException {
		if(owner.isNullable()) throw new IxException(op, "Cannot perform equality check on nullable type ('%s')".formatted(owner));
		methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner.getInternalName(), "equals", "(Ljava/lang/Object;)Z", false);
	}

	private void not(MethodVisitor methodVisitor, Label falseL) {
		methodVisitor.visitJumpInsn(Opcodes.IFNE, falseL);
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
