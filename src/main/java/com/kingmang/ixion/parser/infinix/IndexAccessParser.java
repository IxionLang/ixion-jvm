package com.kingmang.ixion.parser.infinix;

import com.kingmang.ixion.ast.CallExpression;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.IdentifierExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.parser.Precedence;

import java.util.ArrayList;

public record IndexAccessParser() implements InfixParselet {
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        Expression right = parser.expression(precedence());
        parser.consume(TokenType.RBRACK, "Expected ']' after index access.");

        var arguments = new ArrayList<Expression>();
        if (left instanceof IdentifierExpression id) {
            arguments.add(id);
            arguments.add(right);
        }
        return new CallExpression(
                pos,
                new IdentifierExpression(pos, new Token(TokenType.IDENTIFIER, pos.line(), pos.col(), "at")),
                arguments
        );

    }

    @Override
    public int precedence() {
            return Precedence.POSTFIX;
        }
}