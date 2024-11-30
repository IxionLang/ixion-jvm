package com.kingmang.ixion.parser;

import com.kingmang.ixion.parser.tokens.Token;

import java.util.List;

public interface Lexer {
    List<Token> tokenize();
}
