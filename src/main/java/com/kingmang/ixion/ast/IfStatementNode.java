package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.compiler.Variable;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.Pair;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class IfStatementNode implements Node {

	private final Token ifTok;
	private final Node condition;
	private final Node body;
	private final Node elseBody;

	public IfStatementNode(Token ifTok, Node condition, Node body, Node elseBody) {
		this.ifTok = ifTok;
		this.condition = condition;
		this.body = body;
		this.elseBody = elseBody;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		MethodVisitor methodVisitor = context.getContext().getMethodVisitor();

		IxType conditionReturnType = condition.getReturnType(context.getContext());
		if(!conditionReturnType.equals(IxType.BOOLEAN_TYPE)) {
			throw new IxException(ifTok, "Invalid condition type (%s =/= boolean)"
					.formatted(conditionReturnType));
		}

		context.getContext().updateLine(ifTok.line());

		Label falseL = new Label();
		Label end = new Label();

		boolean isNode = false;
		if(condition instanceof IsNode) {
			((IsNode) condition).setShouldReScope(true);
			isNode = true;
		}

		if(condition instanceof EqualityOperationNode) {
			if(!((EqualityOperationNode) condition).generateConditional(context, falseL)) {
				methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseL);
			}
		}
		else if(condition instanceof RelativeOperationNode) {
			((RelativeOperationNode) condition).generateConditional(context, falseL);
		}
		else if(condition instanceof LogicalOperationNode) {
			((LogicalOperationNode) condition).generateConditional(context, falseL);
		}
		else {
			condition.visit(context);
			methodVisitor.visitJumpInsn(Opcodes.IFEQ, falseL);
		}

		body.visit(context);

		if(isNode) {
			Pair<Variable, IxType> pastVariable = ((IsNode) condition).getPastVariable();
			pastVariable.first().setType(pastVariable.second());
		}

		methodVisitor.visitJumpInsn(Opcodes.GOTO, end);
		methodVisitor.visitLabel(falseL);
		if(elseBody != null) {
			elseBody.visit(context);
		}
		methodVisitor.visitLabel(end);
	}

	@Override
	public String toString() {
		return "if(%s) %s".formatted(condition, body) + (elseBody == null ? "" : "else %s".formatted(elseBody));
	}
}
