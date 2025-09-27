package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;

import java.util.List;
import java.util.Optional;

public final class UnionTypeStatement extends TypeStatement {
    public final List<TypeStatement> types;

    public UnionTypeStatement(Position pos, List<TypeStatement> types) {
        super(pos, null, Optional.empty(), true);
        this.types = types;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitUnionType(this);
    }
}