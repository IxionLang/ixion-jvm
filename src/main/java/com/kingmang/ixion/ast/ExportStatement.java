package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;

public final class ExportStatement extends Statement {
    public final Statement stmt;

    public ExportStatement(Position pos, Statement stmt) {
        super(pos);
        this.stmt = stmt;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitExport(this);
    }
}