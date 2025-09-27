package com.kingmang.ixion.parser.prefix;

import com.kingmang.ixion.ast.EmptyListExpression;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.IdentifierExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;

public record IdentifierParser() implements PrefixParselet {
    public Expression parse(Parser parser, Token token) {
        var pos = parser.getPos();

        if (parser.match(TokenType.MODULE)) {
            var nextToken = parser.consume(TokenType.IDENTIFIER, "Expected identifier after module separator `::`.");
            token = new Token(TokenType.IDENTIFIER, pos.line(), pos.col(), token.source() + "::" + nextToken.source());
        }

        if (parser.peek().type() == TokenType.LBRACK) {
            parser.consume();
            parser.consume(TokenType.RBRACK, "Expect ']' to close list constructor.");
            return new EmptyListExpression(pos, token);
        }
        return new IdentifierExpression(pos, token);

    }
}