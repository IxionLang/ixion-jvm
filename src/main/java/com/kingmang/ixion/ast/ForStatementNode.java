package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.optimization.OptimizationUtil;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ForStatementNode implements Node {

	private final Token forTok;
	private final Node init;
	private final Node condition;
	private final Node iterate;
	private final Node body;

	public ForStatementNode(Token forTok, Node init, Node condition, Node iterate, Node body) {
		this.forTok = forTok;
		this.init = init;
		this.condition = condition;
		this.iterate = iterate;
		this.body = body;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType conditionReturnType = condition.getReturnType(context.getContext());
		if(!conditionReturnType.equals(IxType.BOOLEAN_TYPE)) {
			throw new IxException(forTok, "Invalid condition type (%s =/= boolean)"
					.formatted(conditionReturnType));
		}

		Scope outer = context.getContext().getScope();

		context.getContext().setScope(outer.nextDepth());

		init.visit(context);

		Label conditionL = new Label();
		Label step = new Label();
		Label end = new Label();

		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();
		methodVisitor.visitLabel(conditionL);

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

		context.getContext().setCycleStartLabel(step);
		context.getContext().setCycleEndLabel(end);
		body.visit(context);
		context.getContext().setCycleStartLabel(null);
		context.getContext().setCycleEndLabel(null);

		methodVisitor.visitLabel(step);
		boolean varAssign = OptimizationUtil.assignmentNodeExpressionEval(iterate, context);

		IxType iterateType = iterate.getReturnType(context.getContext());

		if(!iterateType.equals(IxType.VOID_TYPE) && !varAssign) methodVisitor.visitInsn(iterateType.getPopOpcode());

		methodVisitor.visitJumpInsn(Opcodes.GOTO, conditionL);

		methodVisitor.visitLabel(end);

		outer.setReturned(context.getContext().getScope().isReturned());
		context.getContext().setScope(outer);
	}

	@Override
	public String toString() {
		return "for(%s %s; %s) %s".formatted(init.toString().replace(";", ";"), condition, iterate, body);
	}

}
