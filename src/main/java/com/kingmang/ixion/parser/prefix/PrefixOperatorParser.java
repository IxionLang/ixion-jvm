package com.kingmang.ixion.parser.prefix;

import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.PrefixExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;

public record PrefixOperatorParser(int precedence) implements PrefixParselet {
    public Expression parse(Parser parser, Token token) {
        var pos = parser.getPos();
        Expression right = parser.expression(precedence());

        return new PrefixExpression(pos, token, right);
    }
}