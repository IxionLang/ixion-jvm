package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class BadExpression extends Expression {
    public final Token[] badTokens;

    public BadExpression(Position pos, Token... badTokens) {
        super(pos);
        this.badTokens = badTokens;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitBad(this);
    }
}