package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.lexer.Position;

import java.util.List;

public final class BlockStatement extends Statement {
    public final List<Statement> statements;
    public final Context context;

    public BlockStatement(Position pos, List<Statement> statements, Context context) {
        super(pos);
        this.statements = statements;
        this.context = context;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitBlockStmt(this);
    }
}