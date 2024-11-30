package com.kingmang.ixion.parser;


import com.kingmang.ixion.exceptions.ParserException;

public interface Parser {
    Node parse() throws ParserException;
}
