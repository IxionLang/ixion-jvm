package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.*;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class CatchNode implements Node {
	private final Token bindingName;
	private final Node exceptionType;
	private final Node body;

	private Label from;
	private Label to;
	private Label handler;

	private Node finallyBlock;
	private Label finallyLabel;
	private Label start;
	private Label end;

	public CatchNode(Token bindingName, Node exceptionType, Node body) {
		this.bindingName = bindingName;
		this.exceptionType = exceptionType;
		this.body = body;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		Scope outer = context.getContext().getScope();

		context.getContext().setScope(outer.nextDepth());

		IxType exception = getExceptionType(context.getContext());

		int varIndex = context.getContext().getScope().nextLocal();
		context.getContext().getScope().addVariable(new Variable(VariableType.LOCAL, bindingName.value(), varIndex, exception, true));

		context.getContext().getMethodVisitor().visitLabel(handler);
		context.getContext().getMethodVisitor().visitLabel(start);
		context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ASTORE, varIndex);

		body.visit(context);
		context.getContext().getMethodVisitor().visitLabel(end);
		if(finallyBlock != null) finallyBlock.visit(context);

		context.getContext().setScope(outer);
	}

	public void updateMetadata(Label from, Label to, Node finallyBlock, Label finallyLabel) {
		this.from = from;
		this.to = to;
		this.finallyBlock = finallyBlock;
		this.finallyLabel = finallyLabel;
	}

	public void generateTryCatchBlock(MethodVisitor visitor, Context context) throws IxException {
		handler = new Label();
		start = new Label();
		end = new Label();
		visitor.visitTryCatchBlock(from, to, handler, getExceptionType(context).getInternalName());
		if(finallyBlock != null) {
			visitor.visitTryCatchBlock(start, end, finallyLabel, null);
		}
	}

	private IxType getExceptionType(Context context) throws IxException {
		IxType exception = exceptionType.getReturnType(context);

		if(exception.isPrimitive()) {
			throw new IxException(bindingName, "Cannot throw primitive type (got '%s').".formatted(exception));
		}

		try {
			if(!IxType.getObjectType("java/lang/Throwable").isAssignableFrom(exception, context, false)) {
				throw new IxException(bindingName, "throw target must be an extension of java.lang.Throwable ('%s' cannot be cast).".formatted(exception));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(bindingName, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		return exception;
	}

	@Override
	public String toString() {
		return "catch(%s: %s) %s".formatted(bindingName.value(), exceptionType, body);
	}
}
