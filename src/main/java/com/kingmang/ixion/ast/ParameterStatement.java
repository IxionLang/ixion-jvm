package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class ParameterStatement extends Statement {
    public final Token name;
    public final TypeStatement type;

    public ParameterStatement(Position pos, Token name, TypeStatement type) {
        super(pos);
        this.name = name;
        this.type = type;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitParameterStmt(this);
    }
}