package com.kingmang.ixion.ast;

import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.util.FileContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class UnitTestNode implements Node {

    Token unittestToken;
    Node body;

    @Override
    public void visit(FileContext context) throws IxException {}
}
