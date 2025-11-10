package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;

import java.util.Optional;

import static com.kingmang.ixion.lexer.TokenType.TYPEALIAS;

public class TypeStatement extends Statement {
    public final Token identifier;
    public final Optional<TypeStatement> next;
    public final boolean listType;

    public TypeStatement(Position pos, Token identifier, Optional<TypeStatement> next, boolean listType) {
        super(pos);
        this.identifier = identifier;
        this.next = next;
        this.listType = listType;
    }

    public static TypeStatement voidInstance(Position loc) {
        return new TypeStatement(loc, new Token(TYPEALIAS, loc.line(), loc.col(), "void"), Optional.empty(), false);
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitTypeAlias(this);
    }
}