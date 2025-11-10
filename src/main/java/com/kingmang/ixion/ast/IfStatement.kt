package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;

public final class IfStatement extends Statement {
    public final BlockStatement trueBlock;
    public final Statement falseStatement;
    public final Expression condition;

    public IfStatement(Position pos, Expression condition, BlockStatement trueBlock, Statement falseStatement) {
        super(pos);
        this.condition = condition;
        this.trueBlock = trueBlock;
        this.falseStatement = falseStatement;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitIf(this);
    }
}