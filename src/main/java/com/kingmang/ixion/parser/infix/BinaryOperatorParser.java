package com.kingmang.ixion.parser.infix;

import com.kingmang.ixion.ast.BinaryExpression;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;

public record BinaryOperatorParser(int precedence, boolean isRight) implements InfixParselet {

    @Override
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        Expression right = parser.expression(
                precedence - (isRight ? 1 : 0));
        return new BinaryExpression(pos, left, token, right);
    }
}