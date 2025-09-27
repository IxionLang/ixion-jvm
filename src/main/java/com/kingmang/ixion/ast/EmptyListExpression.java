package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public class EmptyListExpression extends Expression {
    public final Token tokenType;

    public EmptyListExpression(Position pos, Token tokenType) {
        super(pos);
        this.tokenType = tokenType;
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitEmptyList(this);
    }
}