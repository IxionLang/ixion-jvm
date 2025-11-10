package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class TypeAliasStatement extends Statement implements PublicAccess {
    public final Token identifier;
    public final TypeStatement typeStmt;

    public TypeAliasStatement(Position pos, Token identifier, TypeStatement typeStmt) {
        super(pos);
        this.identifier = identifier;
        this.typeStmt = typeStmt;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitTypeAlias(this);
    }

    @Override
    public String identifier() {
        return identifier.source();
    }
}