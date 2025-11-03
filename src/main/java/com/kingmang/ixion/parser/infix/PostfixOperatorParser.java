package com.kingmang.ixion.parser.infix;

import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.PostfixExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;

public record PostfixOperatorParser(int precedence) implements InfixParselet {
    public Expression parse(Parser parser, Expression left, Token token) {
        var pos = parser.getPos();
        return new PostfixExpression(pos, left, token);
    }
}