package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.stream.Collectors;

public class NullableMethodCallNode implements Node {

	private final Node left;
	private final Token name;
	private final List<Node> args;
	private final boolean isSuper;

	public NullableMethodCallNode(Node left, Token name, List<Node> args, boolean isSuper) {
		this.left = left;
		this.name = name;
		this.args = args;
		this.isSuper = isSuper;
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

		synthetic().visitCall(context);
		synthetic().getReturnType(context.getContext()).autoBox(context.getContext().getMethodVisitor());

		context.getContext().getMethodVisitor().visitLabel(nullValue);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType rawType = synthetic().getReturnType(context);

		return rawType.getAutoBoxWrapper().asNullable();
	}

	private MethodCallNode synthetic() {
		return new MethodCallNode(left, name, args, isSuper);
	}

	@Override
	public String toString() {
		return "%s?.%s(%s)".formatted(left, name.value(), args.stream().map(Node::toString).collect(Collectors.joining(", ")));
	}
}
