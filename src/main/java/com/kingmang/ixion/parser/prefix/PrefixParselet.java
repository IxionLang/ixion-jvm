package com.kingmang.ixion.parser.prefix;

import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.parser.Parser;

public interface PrefixParselet {
    Expression parse(Parser parser, Token token);
}
