package com.kingmang.ixion.parser.infix;

import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;

public interface InfixParselet {
    Expression parse(Parser parser, Expression left, Token token);

    int precedence();

}
