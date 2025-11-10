package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

public final class AssignExpression extends Expression {
    public final Expression left;
    public final Expression right;

    public AssignExpression(Position pos, Expression left, Expression right) {
        super(pos);
        this.left = left;
        this.right = right;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitAssignExpr(this);
    }
}