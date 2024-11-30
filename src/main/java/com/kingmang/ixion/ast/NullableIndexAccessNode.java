package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

public class NullableIndexAccessNode implements Node {

	private final Token bracket;
	private final Node left;
	private final Node index;

	public NullableIndexAccessNode(Token bracket, Node left, Node index) {
		this.bracket = bracket;
		this.left = left;
		this.index = index;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType returnType = left.getReturnType(context.getContext());
		if(!returnType.isNullable()) {
			throw new IxException(bracket, "Cannot use '?[' on non-nullable type ('%s')".formatted(left.getReturnType(context.getContext())));
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

	private IndexAccessNode synthetic() {
		return new IndexAccessNode(bracket, left, index);
	}

	@Override
	public Value getLValue() {
		return Value.NULLABLE_ARRAY;
	}

	@Override
	public Object[] getLValueData() {
		return new Object[] { left, index, bracket };
	}

	@Override
	public String toString() {
		return "%s?[%s]".formatted(left, index);
	}
}
