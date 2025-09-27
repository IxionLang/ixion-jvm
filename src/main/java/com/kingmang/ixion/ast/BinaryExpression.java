package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class BinaryExpression extends Expression {
    public final Expression left;
    public final Token operator;
    public final Expression right;

    public BinaryExpression(Position pos, Expression left, Token operator, Expression right) {
        super(pos);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}