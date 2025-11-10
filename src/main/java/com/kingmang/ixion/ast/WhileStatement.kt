package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;

public final class WhileStatement extends Statement {
    public final BlockStatement block;
    public final Expression condition;

    public WhileStatement(Position pos, Expression condition, BlockStatement block) {
        super(pos);
        this.condition = condition;
        this.block = block;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitWhile(this);
    }
}