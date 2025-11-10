package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.List;

public final class StructStatement extends Statement implements PublicAccess {
    public final Token name;
    public final List<ParameterStatement> fields;
    public final List<Token> generics;

    public StructStatement(Position pos, Token name, List<ParameterStatement> fields, List<Token> generics) {
        super(pos);
        this.name = name;
        this.fields = fields;
        this.generics = generics;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitStruct(this);
    }

    @Override
    public String identifier() {
        return name.source();
    }
}