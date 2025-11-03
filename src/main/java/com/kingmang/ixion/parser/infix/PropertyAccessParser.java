package com.kingmang.ixion.parser.infix;

import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.IdentifierExpression;
import com.kingmang.ixion.ast.PropertyAccessExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.parser.Precedence;

import java.util.ArrayList;
import java.util.List;

public record PropertyAccessParser() implements InfixParselet {

    @Override
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        List<IdentifierExpression> identifiers = new ArrayList<>();

        var i = parser.consume();
        identifiers.add(new IdentifierExpression(parser.getPos(), i));
        while (parser.peek().type() == TokenType.DOT) {
            parser.consume();
            if (parser.peek().type() == TokenType.IDENTIFIER) {
                i = parser.consume();
                identifiers.add(new IdentifierExpression(parser.getPos(), i));
            }
        }

        return new PropertyAccessExpression(pos, left, identifiers);
    }

    @Override
    public int precedence() {
            return Precedence.PREFIX;
        }
}