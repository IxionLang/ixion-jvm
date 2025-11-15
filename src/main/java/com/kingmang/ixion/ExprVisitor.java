package com.kingmang.ixion;

import com.kingmang.ixion.ast.*;
import org.jetbrains.annotations.NotNull;

public interface ExprVisitor<R> {
    @NotNull R visitAssignExpr(AssignExpression expr);
    @NotNull R visitBad(BadExpression expr);
    @NotNull R visitBinaryExpr(BinaryExpression expr);
    @NotNull R visitCall(CallExpression expr);
    @NotNull R visitEmpty(EmptyExpression empty);
    @NotNull R visitEmptyList(EmptyListExpression emptyList);
    @NotNull R visitGroupingExpr(GroupingExpression expr);
    @NotNull R visitIdentifierExpr(IdentifierExpression expr);
    @NotNull R visitIndexAccess(IndexAccessExpression expr);
    @NotNull R visitLiteralExpr(LiteralExpression expr);
    @NotNull R visitLiteralList(LiteralListExpression expr);
    @NotNull R visitModuleAccess(ModuleAccessExpression expr);
    @NotNull R visitPostfixExpr(PostfixExpression expr);
    @NotNull R visitPrefix(PrefixExpression expr);
    @NotNull R visitPropertyAccess(PropertyAccessExpression expr);
    @NotNull R visitLambda(@NotNull LambdaExpression expression);
}
