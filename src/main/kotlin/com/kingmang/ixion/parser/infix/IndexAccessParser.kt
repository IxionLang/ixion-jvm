package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.*
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class IndexAccessParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        if (left is IdentifierExpression && parser.isTypeApplication()) {
            // '[' has already been consumed by the Pratt loop.
            val types = arrayListOf(parser.parseUnion())
            while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RBRACK)) types.add(parser.parseUnion())
            parser.consume(TokenType.RBRACK, "Expected ']' after type arguments.")
            var name = left.identifier
            if (parser.match(TokenType.MODULE)) {
                val variant = parser.consume(TokenType.IDENTIFIER, "Expected variant name.")
                name = Token(TokenType.IDENTIFIER, name.line, name.col, name.source + "::" + variant.source)
            }
            return TypeApplicationExpression(left.position!!, name, types)
        }
        val right = parser.expression()
        parser.consume(TokenType.RBRACK, "Expected ']' after index access.")
        return IndexAccessExpression(left.position, left, right)
    }

    override val precedence: Int
        get() = Precedence.POSTFIX
}
