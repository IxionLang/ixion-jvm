package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class NonNullAssertionNode implements Node {

	private final Node target;
	private final Token op;

	public NonNullAssertionNode(Node target, Token op) {
		this.target = target;
		this.op = op;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		target.visit(context);

		IxType returnType = target.getReturnType(context.getContext());
		if(returnType.isPrimitive()) {
			throw new IxException(op, "Cannot assert non-null on primitive type '%s'".formatted(returnType));
		}
		if(!returnType.isNullable()) {
			throw new IxException(op, "Cannot assert non-null on type which is already not null ('%s')".formatted(returnType));
		}

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		visitor.visitInsn(returnType.getDupOpcode());

		Label nonNullBranch = new Label();

		visitor.visitJumpInsn(Opcodes.IFNONNULL, nonNullBranch);

		visitor.visitTypeInsn(Opcodes.NEW, "java/lang/NullPointerException");
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "()V", false);

		visitor.visitInsn(Opcodes.ATHROW);

		visitor.visitLabel(nonNullBranch);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType type = target.getReturnType(context);

		if(type.isPrimitive()) {
			throw new IxException(op, "Cannot assert non-null on primitive type '%s'".formatted(type));
		}
		if(!type.isNullable()) {
			throw new IxException(op, "Cannot assert non-null on type which is already not null ('%s')".formatted(type));
		}

		return type.copy().asNonNullable();
	}

	@Override
	public String toString() {
		return target + "!";
	}
}
