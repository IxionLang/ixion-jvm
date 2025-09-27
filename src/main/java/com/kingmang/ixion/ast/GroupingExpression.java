package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

public final class GroupingExpression extends Expression {
    public final Expression expression;

    public GroupingExpression(Position pos, Expression expression) {
        super(pos);
        this.expression = expression;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitGroupingExpr(this);
    }
}