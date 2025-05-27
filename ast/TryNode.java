package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.stream.Collectors;

public class TryNode implements Node {

	private final Node body;
	private final List<CatchNode> catchBlocks;
	private final Node finallyBlock;

	public TryNode(Node body, List<CatchNode> catchBlocks, Node finallyBlock) {
		this.body = body;
		this.catchBlocks = catchBlocks;
		this.finallyBlock = finallyBlock;
	}


	@Override
	public void visit(FileContext context) throws IxException {
		Label from = new Label();
		Label to = new Label();
		Label end = new Label();
		Label finallyLabel = new Label();

		MethodVisitor visitor = context.getContext().getMethodVisitor();

		for(CatchNode catchNode : catchBlocks) {
			catchNode.updateMetadata(from, to, finallyBlock, finallyLabel);
			catchNode.generateTryCatchBlock(visitor, context.getContext());
		}

		if(finallyBlock != null) {
			visitor.visitTryCatchBlock(from, to, finallyLabel, null);
		}

		visitor.visitLabel(from);

		body.visit(context);
		if(finallyBlock != null) {
			finallyBlock.visit(context);
		}

		visitor.visitLabel(to);

		for(CatchNode catchNode : catchBlocks) {
			context.getContext().getMethodVisitor().visitJumpInsn(Opcodes.GOTO, end);
			catchNode.visit(context);
		}

		if(finallyBlock != null) {
			Scope outer = context.getContext().getScope();

			context.getContext().setScope(outer.nextDepth());

			int varIndex = context.getContext().getScope().nextLocal();

			context.getContext().getMethodVisitor().visitJumpInsn(Opcodes.GOTO, end);

			context.getContext().getMethodVisitor().visitLabel(finallyLabel);

			context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ASTORE, varIndex);

			finallyBlock.visit(context);

			context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, varIndex);

			context.getContext().getMethodVisitor().visitInsn(Opcodes.ATHROW);

			context.getContext().setScope(outer);
		}

		visitor.visitLabel(end);
	}

	@Override
	public String toString() {
		return "try %s%s%s".formatted(body,
				catchBlocks.stream().map(Node::toString).collect(Collectors.joining()),
				finallyBlock == null ? "" : "finally " + finallyBlock);
	}
}
