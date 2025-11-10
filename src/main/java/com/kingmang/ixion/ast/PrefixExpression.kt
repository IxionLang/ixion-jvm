package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class PrefixExpression extends Expression {
    public final Token operator;
    public final Expression right;

    public PrefixExpression(Position pos, Token operator, Expression right) {
        super(pos);
        this.operator = operator;
        this.right = right;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitPrefix(this);
    }
}