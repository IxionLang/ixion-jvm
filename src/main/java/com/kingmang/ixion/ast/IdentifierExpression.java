package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public class IdentifierExpression extends Expression {
    public final Token identifier;

    public IdentifierExpression(Position pos, Token identifier) {
        super(pos);
        this.identifier = identifier;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitIdentifierExpr(this);
    }
}