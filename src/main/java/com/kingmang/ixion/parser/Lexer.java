package com.kingmang.ixion.parser;

import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenStream;

import java.util.Iterator;
import java.util.List;

public interface Lexer extends Iterator<Token> {
    TokenStream tokenize();
}
