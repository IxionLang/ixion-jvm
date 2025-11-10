package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

public class IndexAccessExpression extends Expression {
    public final Expression left;
    public final Expression right;

    public IndexAccessExpression(Position pos, Expression left, Expression right) {
        super(pos);
        this.left = left;
        this.right = right;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitIndexAccess(this);
    }
}