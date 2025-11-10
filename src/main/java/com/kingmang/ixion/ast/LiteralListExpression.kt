package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

import java.util.List;

public final class LiteralListExpression extends Expression {
    public final List<Expression> entries;

    public LiteralListExpression(Position pos, List<Expression> entries) {
        super(pos);
        this.entries = entries;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitLiteralList(this);
    }
}