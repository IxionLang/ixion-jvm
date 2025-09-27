package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

public final class EmptyExpression extends Expression {
    public EmptyExpression(Position pos) {
        super(pos);
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitEmpty(this);
    }
}