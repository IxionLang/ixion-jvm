package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class PostfixExpression extends Expression {
    public final Expression expression;
    public final Token operator;
    public int localIndex = -1;

    public PostfixExpression(Position pos, Expression expression, Token operator) {
        super(pos);
        this.expression = expression;
        this.operator = operator;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitPostfixExpr(this);
    }
}