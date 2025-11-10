package com.kingmang.ixion.ast;

import com.kingmang.ixion.ExprVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.runtime.IxType;
import com.kingmang.ixion.runtime.UnknownType;

public abstract class Expression implements Node {
    public final Position pos;
    public IxType realType = new UnknownType("unknown");

    protected Expression(Position pos) {
        this.pos = pos;
    }

    public abstract <R> R accept(ExprVisitor<R> visitor);

    @Override
    public Position pos() {
        return pos;
    }
}