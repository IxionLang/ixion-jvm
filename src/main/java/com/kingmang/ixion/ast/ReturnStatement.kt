package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;

public final class ReturnStatement extends Statement {
    public final Expression expression;

    public ReturnStatement(Position pos, Expression expression) {
        super(pos);
        this.expression = expression;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitReturnStmt(this);
    }
}