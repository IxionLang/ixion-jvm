package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class WhileStatementNode implements Node {

	private final Token whileTok;
	private final Node condition;
	private final Node body;

	public WhileStatementNode(Token whileTok, Node condition, Node body) {
		this.whileTok = whileTok;
		this.condition = condition;
		this.body = body;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		IxType conditionReturnType = condition.getReturnType(context.getContext());
		if(!conditionReturnType.equals(IxType.BOOLEAN_TYPE)) {
			throw new IxException(whileTok, "Invalid condition type (%s =/= boolean)"
					.formatted(conditionReturnType));
		}

		context.getContext().updateLine(whileTok.line());

		Label conditionLabel = new Label();
		Label end = new Label();

		methodVisitor.visitLabel(conditionLabel);

        switch (condition) {
            case EqualityOperationNode equalityOperationNode -> {
                if (!equalityOperationNode.generateConditional(context, end)) {
                    methodVisitor.visitJumpInsn(Opcodes.IFEQ, end);
                }
            }
            case RelativeOperationNode relativeOperationNode -> relativeOperationNode.generateConditional(context, end);
            case LogicalOperationNode logicalOperationNode -> logicalOperationNode.generateConditional(context, end);
            default -> {
                condition.visit(context);
                methodVisitor.visitJumpInsn(Opcodes.IFEQ, end);
            }
        }

		context.getContext().setCycleStartLabel(conditionLabel);
		context.getContext().setCycleEndLabel(end);
		body.visit(context);
		context.getContext().setCycleStartLabel(null);
		context.getContext().setCycleEndLabel(null);

		methodVisitor.visitJumpInsn(Opcodes.GOTO, conditionLabel);

		methodVisitor.visitLabel(end);
	}

	@Override
	public String toString() {
		return "while(%s) %s".formatted(condition, body);
	}
}
