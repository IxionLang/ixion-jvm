package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.List;

public final class EnumStatement extends Statement implements PublicAccess {
    public final Token name;
    public final List<Token> values;

    public EnumStatement(Position pos, Token name, List<Token> values) {
        super(pos);
        this.name = name;
        this.values = values;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitEnum(this);
    }

    @Override
    public String identifier() {
        return name.source();
    }
}