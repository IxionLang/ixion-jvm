package com.kingmang.ixion;

import com.kingmang.ixion.ast.*;

public interface ExprVisitor<R> {
    R visitAssignExpr(AssignExpression expr);
    R visitBad(BadExpression expr);
    R visitBinaryExpr(BinaryExpression expr);
    R visitCall(CallExpression expr);
    R visitEmpty(EmptyExpression empty);
    R visitEmptyList(EmptyListExpression emptyList);
    R visitGroupingExpr(GroupingExpression expr);
    R visitIdentifierExpr(IdentifierExpression expr);
    R visitIndexAccess(IndexAccessExpression expr);
    R visitLiteralExpr(LiteralExpression expr);
    R visitLiteralList(LiteralListExpression expr);
    R visitModuleAccess(ModuleAccessExpression expr);
    R visitPostfixExpr(PostfixExpression expr);
    R visitPrefix(PrefixExpression expr);
    R visitPropertyAccess(PropertyAccessExpression expr);
    //R visit(LambdaExpression expr);
}
