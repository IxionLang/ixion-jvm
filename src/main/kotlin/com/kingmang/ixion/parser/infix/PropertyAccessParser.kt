package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.IdentifierExpression
import com.kingmang.ixion.ast.PropertyAccessExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class PropertyAccessParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val pos = parser.pos

        val identifiers = ArrayList<IdentifierExpression>()
        var i = parser.consume(TokenType.IDENTIFIER, "Expected field name after dot.")
        identifiers.add(IdentifierExpression(parser.pos, i))

        while (parser.peek().type == TokenType.DOT) {
            parser.consume()
            if (parser.peek().type == TokenType.IDENTIFIER) {
                i = parser.consume()
                identifiers.add(IdentifierExpression(parser.pos, i))
            }
        }

        return PropertyAccessExpression(pos, left, identifiers)
    }

    override val precedence: Int
        get() = Precedence.PREFIX
}