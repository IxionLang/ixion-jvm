package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IntegerOperationNode implements Node {

	private final Node left;
	private final Token op;
	private final Node right;

	public IntegerOperationNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType rightType = right.getReturnType(context.getContext());

		if(!leftType.isInteger() || !rightType.isInteger()) {
			throw new IxException(op, "Unsupported operation of '%s' between types '%s' and '%s'".formatted(
					op.value(), leftType, rightType
			));
		}

		MethodVisitor visitor = context.getContext().getMethodVisitor();

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

	private int getOpcode() {
		return switch (op.type()) {
			case BITWISE_OR -> Opcodes.IOR;
			case BITWISE_XOR -> Opcodes.IXOR;
			case BITWISE_AND -> Opcodes.IAND;
			case BITWISE_SHL -> Opcodes.ISHL;
			case BITWISE_SHR -> Opcodes.ISHR;
			case BITWISE_USHR -> Opcodes.IUSHR;
			default -> throw new IllegalStateException("Invalid operation of '%s' in IntegerOperationNode".formatted(op.type()));
		};
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return left.getReturnType(context).getLarger(right.getReturnType(context));
	}

	@Override
	public String toString() {
		return "%s %s %s".formatted(left, op.value(), right);
	}
}
