package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class NullableMemberAccessNode implements Node {

	private final Node left;
	private final Token name;

	public NullableMemberAccessNode(Node left, Token name) {
		this.left = left;
		this.name = name;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType returnType = left.getReturnType(context.getContext());
		if(!returnType.isNullable()) {
			throw new IxException(name, "Cannot use '?.' on non-nullable type ('%s')".formatted(left.getReturnType(context.getContext())));
		}

		Label nullValue = new Label();

		boolean isSettingJump = false;
		if(context.getContext().getNullJumpLabel(nullValue) == nullValue) {
			context.getContext().setNullJumpLabel(nullValue);
			isSettingJump = true;
		}

		left.visit(context);

		if(isSettingJump) {
			context.getContext().setNullJumpLabel(null);
		}

		context.getContext().getMethodVisitor().visitInsn(Opcodes.DUP);
		context.getContext().getMethodVisitor().visitJumpInsn(Opcodes.IFNULL, context.getContext().getNullJumpLabel(nullValue));

		synthetic().visitAccess(context);
		synthetic().getReturnType(context.getContext()).autoBox(context.getContext().getMethodVisitor());

		context.getContext().getMethodVisitor().visitLabel(nullValue);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType rawType = synthetic().getReturnType(context);

		return rawType.getAutoBoxWrapper().asNullable();
	}

	private MemberAccessNode synthetic() {
		return new MemberAccessNode(left, name);
	}

	@Override
	public Value getLValue() {
		return Value.NULLABLE_PROPERTY;
	}

	@Override
	public Object[] getLValueData() {
		return new Object[] { left, name };
	}

	@Override
	public String toString() {
		return "%s?.%s".formatted(left, name.value());
	}
}
