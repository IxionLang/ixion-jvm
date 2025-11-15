package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Parser

class LambdaParser : PrefixParselet {
    override fun parse(parser: Parser, token: Token?): Expression? {
        return parser.parseLambda()
    }
}