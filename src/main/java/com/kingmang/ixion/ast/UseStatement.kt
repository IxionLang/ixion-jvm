package com.kingmang.ixion.ast;

import com.kingmang.ixion.StatementVisitor;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;

import java.util.Optional;

public final class UseStatement extends Statement implements Statement.TopLevel {
    public final static UseStatement instance = new UseStatement(
            new Position(0, 0),
            new Token(TokenType.STRING, 0, 0, "prelude"),
            Optional.empty());

    public final Token stringLiteral;
    public final Optional<Token> identifier;

    public UseStatement(Position pos, Token stringLiteral, Optional<Token> identifier) {
        super(pos);
        this.stringLiteral = stringLiteral;
        this.identifier = identifier;
    }

    @Override
    public <R> R accept(StatementVisitor<R> visitor) {
        return visitor.visitUse(this);
    }
}