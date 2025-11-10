package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.List;

public final class CallExpression extends Expression {
    public final Expression item;
    public final List<Expression> arguments;
    public Token foreign;

    public CallExpression(Position pos, Expression item, List<Expression> arguments) {
        super(pos);
        this.item = item;
        this.arguments = arguments;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitCall(this);
    }
}