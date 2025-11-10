package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.parser.Node;

public abstract class Statement implements Node {
    public final Position pos;

    protected Statement(Position pos) {
        this.pos = pos;
    }

    public abstract <R> R accept(StatementVisitor<R> visitor);

    @Override
    public Position pos() {
        return pos;
    }

    public sealed interface TopLevel permits UseStatement {
    }
}