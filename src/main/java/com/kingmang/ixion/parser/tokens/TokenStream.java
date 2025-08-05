package com.kingmang.ixion.parser.tokens;

import java.util.ArrayList;
import java.util.List;

public class TokenStream {
    List<Token> tokenList;

    public TokenStream(){
        this.tokenList = new ArrayList<>();
    }

    public void append(Token token){
        tokenList.add(token);
    }

    public Token get(int index){
        return tokenList.get(index);
    }

    public int size(){
        return tokenList.size();
    }

}
