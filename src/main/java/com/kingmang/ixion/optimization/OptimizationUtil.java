package com.kingmang.ixion.optimization;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.ast.AssignmentNode;

public class OptimizationUtil {

	public static boolean assignmentNodeExpressionEval(Node expression, FileContext context) throws IxException {
		boolean varAssign = expression instanceof AssignmentNode;

		if(varAssign) ((AssignmentNode) expression).setExpressionStatementBody(true);

		expression.visit(context);

		return varAssign;
	}

}
