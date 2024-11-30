package com.kingmang.ixion.ast;

import com.kingmang.ixion.optimization.OptimizationUtil;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.types.IxType;

public class ExpressionStatementNode implements Node {
	private final Node expression;

	public ExpressionStatementNode(Node expression) {
		this.expression = expression;
	}

	@Override
	public String toString() {
		return expression + ";";
	}

	@Override
	public void visit(FileContext context) throws IxException {
		if(expression.isConstant(context.getContext())) return;

		boolean varAssign = OptimizationUtil.assignmentNodeExpressionEval(expression, context);

		IxType returnType = expression.getReturnType(context.getContext());
		if(!returnType.equals(IxType.VOID_TYPE) && !varAssign) context.getContext().getMethodVisitor().visitInsn(returnType.getPopOpcode());
	}
}
