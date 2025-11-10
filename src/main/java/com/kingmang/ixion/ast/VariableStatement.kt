package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.Optional;

public final class VariableStatement extends Statement implements PublicAccess {
    public final Token name;
    public final Expression expression;
    public final Token mutability;
    public int localIndex = -1;
    public final Optional<TypeStatement> type;

    public VariableStatement(Position pos, Token mutability, Token name, Expression expression, Optional<TypeStatement> type) {
        super(pos);
        this.mutability = mutability;
        this.name = name;
        this.expression = expression;
        this.type = type;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitVariable(this);
    }

    @Override
    public String identifier() {
        return name.source();
    }
}