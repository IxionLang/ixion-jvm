package com.kingmang.ixion.parser.prefix;

import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.LiteralExpression;
import com.kingmang.ixion.ast.LiteralListExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;

import java.util.ArrayList;
import java.util.List;

public record LiteralParser(boolean isList) implements PrefixParselet {
    public Expression parse(Parser parser, Token token) {
        var pos = parser.getPos();
        if (token.type() == TokenType.LBRACK) {
            List<Expression> args = new ArrayList<>();
            if (!parser.match(TokenType.RBRACK)) {
                do {
                    args.add(parser.expression());
                } while (parser.match(TokenType.COMMA));
                parser.optional(TokenType.COMMA);
                parser.consume(TokenType.RBRACK, "Expected closing ']' after list literal.");
            }
            return new LiteralListExpression(pos, args);
        }

        return new LiteralExpression(pos, token);
    }
}