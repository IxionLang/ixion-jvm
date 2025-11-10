package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.runtime.IxType;

import java.util.ArrayList;
import java.util.List;

public final class PropertyAccessExpression extends Expression {
    public final Expression expression;
    public final List<IdentifierExpression> identifiers;
    public List<IxType> typeChain = new ArrayList<>();

    public PropertyAccessExpression(Position pos, Expression expression, List<IdentifierExpression> identifiers) {
        super(pos);
        this.expression = expression;
        this.identifiers = identifiers;
    }

    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitPropertyAccess(this);
    }
}