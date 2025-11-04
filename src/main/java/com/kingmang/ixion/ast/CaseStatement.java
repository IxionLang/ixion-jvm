package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.runtime.IxType;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;

public final class CaseStatement extends Statement {
    public final Map<TypeStatement, Pair<String, BlockStatement>> cases;
    public final Expression expression;
    public final Map<TypeStatement, IxType> types = new HashMap<>();

    public CaseStatement(Position pos, Expression expression, Map<TypeStatement, Pair<String, BlockStatement>> cases) {
        super(pos);
        this.expression = expression;
        this.cases = cases;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitMatch(this);
    }
}