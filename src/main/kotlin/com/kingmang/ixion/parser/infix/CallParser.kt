package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.CallExpression
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class CallParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos
        val args = ArrayList<Expression>()

        if (!parser.match(TokenType.RPAREN)) {
            do {
                args.add(parser.expression())
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RPAREN))
            parser.optional(TokenType.COMMA)
            parser.consume(TokenType.RPAREN, "Expected closing ')' after function call.")
        }
        return CallExpression(pos, left, args)
    }

    override val precedence: Int
        get() = Precedence.PRIMARY
}