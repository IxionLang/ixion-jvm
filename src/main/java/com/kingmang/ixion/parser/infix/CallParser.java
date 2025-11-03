package com.kingmang.ixion.parser.infix;

import com.kingmang.ixion.ast.CallExpression;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.parser.Precedence;

import java.util.ArrayList;
import java.util.List;

public record CallParser() implements InfixParselet {
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        List<Expression> args = new ArrayList<>();

        if (!parser.match(TokenType.RPAREN)) {
            do {
                args.add(parser.expression());
            } while (parser.match(TokenType.COMMA));
            parser.optional(TokenType.COMMA);
            parser.consume(TokenType.RPAREN, "Expected closing ')' after function call.");
        }
        return new CallExpression(pos, left, args);
    }

    @Override
    public int precedence() {
            return Precedence.PRIMARY;
        }
}