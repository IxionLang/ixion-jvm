package com.kingmang.ixion.parser.prefix;

import com.kingmang.ixion.ast.Expression;
import com.kingmang.ixion.ast.GroupingExpression;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.Parser;

public record GroupingParser() implements PrefixParselet {
    public Expression parse(Parser parser, Token token) {
        var pos = parser.getPos();
        Expression expression = parser.expression();
        parser.consume(TokenType.RPAREN, "Expected opening parentheses.");
        return new GroupingExpression(pos, expression);
    }
}