package com.kingmang.ixion.parser.infinix;

import com.kingmang.ixion.ast.AssignExpression;
import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;
import com.kingmang.ixion.parser.Precedence;

public record AssignOperatorParser() implements InfixParselet {
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        Expression right = parser.expression(precedence() - 1);
        return new AssignExpression(pos, left, right);
    }

    @Override
    public int precedence() {
            return Precedence.ASSIGNMENT;
        }
}