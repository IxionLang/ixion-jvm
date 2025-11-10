package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;

public final class ModuleAccessExpression extends Expression {
    public final IdentifierExpression identifier;
    public final Expression foreign;

    public ModuleAccessExpression(Position pos, IdentifierExpression identifier, Expression foreign) {
        super(pos);
        this.identifier = identifier;
        this.foreign = foreign;
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitModuleAccess(this);
    }
}