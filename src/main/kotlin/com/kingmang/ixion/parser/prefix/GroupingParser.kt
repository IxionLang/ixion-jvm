package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.*
import com.kingmang.ixion.lexer.*
import com.kingmang.ixion.parser.Parser

class GroupingParser : PrefixParselet {
    override fun parse(parser: Parser, token: Token): Expression {
        val pos = Position(token.line, token.col)
        if (parser.match(TokenType.RPAREN)) return UnitExpression(pos)
        val first = parser.expression()
        if (!parser.match(TokenType.COMMA)) {
            parser.consume(TokenType.RPAREN, "Expected ')'.")
            return GroupingExpression(pos, first)
        }
        val elements = arrayListOf(first)
        while (!parser.check(TokenType.RPAREN)) {
            elements.add(parser.expression())
            if (!parser.match(TokenType.COMMA)) break
        }
        parser.consume(TokenType.RPAREN, "Expected ')' after tuple.")
        return TupleExpression(pos, elements)
    }
}
