package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class LogicalNullOperatorNode implements Node {

	private final Node left;
	private final Token op;
	private final Node right;

	public LogicalNullOperatorNode(Node left, Token op, Node right) {
		this.left = left;
		this.op = op;
		this.right = right;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());

		if(!leftType.isNullable() || leftType.isPrimitive()) {
			throw new IxException(op, "Cannot perform '??' on a non-nullable type ('%s')".formatted(leftType));
		}

		left.visit(context);

		Label end = new Label();

		context.getContext().getMethodVisitor().visitInsn(leftType.getDupOpcode());
		context.getContext().getMethodVisitor().visitJumpInsn(Opcodes.IFNONNULL, end);

		context.getContext().getMethodVisitor().visitInsn(leftType.getPopOpcode());
		right.visit(context);

		context.getContext().getMethodVisitor().visitLabel(end);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType leftType = left.getReturnType(context);
		IxType rightType = right.getReturnType(context);

		if(!leftType.isNullable() || leftType.isPrimitive()) {
			throw new IxException(op, "Cannot perform '??' on a non-nullable type ('%s')".formatted(leftType));
		}

		IxType nonNullableLeft = leftType.asNonNullable();

		try {
			if(!nonNullableLeft.isAssignableFrom(rightType, context, false)) {
				throw new IxException(op, "Cannot perform '??' on types '%s' and '%s'".formatted(leftType, rightType));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(op, "Cannot resolve class '%s'".formatted(e.getMessage()));
		}

		return nonNullableLeft;
	}

	@Override
	public String toString() {
		return "%s ?? %s".formatted(left, right);
	}
}
