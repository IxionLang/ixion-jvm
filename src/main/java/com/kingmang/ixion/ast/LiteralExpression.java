package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class LiteralExpression extends Expression {
    public final Token literal;

    public LiteralExpression(Position pos, Token literal) {
        super(pos);
        this.literal = literal;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitLiteralExpr(this);
    }
}