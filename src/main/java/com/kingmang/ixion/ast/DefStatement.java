package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.List;

public final class DefStatement extends Statement implements PublicAccess {
    public final Token name;
    public final BlockStatement body;
    public final List<ParameterStatement> parameters;
    public final TypeStatement returnType;
    public final List<Token> generics;

    public DefStatement(Position pos, Token name, List<ParameterStatement> parameters, TypeStatement returnType,
                        BlockStatement body, List<Token> generics) {
        super(pos);
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.body = body;
        this.generics = generics;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitFunctionStmt(this);
    }

    @Override
    public String identifier() {
        return name.source();
    }
}