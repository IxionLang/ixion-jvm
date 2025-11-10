package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

public final class ForStatement extends Statement {
    public final BlockStatement block;
    public final Token name;
    public final Expression expression;
    public int localNameIndex = -1;
    public int localExprIndex = -1;

    public ForStatement(Position pos, Token name, Expression expression, BlockStatement block) {
        super(pos);
        this.block = block;
        this.name = name;
        this.expression = expression;
    }

    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitFor(this);
    }
}