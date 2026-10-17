package com.kingmang.ixion.parser.infix

import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.IndexAccessExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser
import com.kingmang.ixion.parser.Precedence

class IndexAccessParser : InfixParselet {
    override fun parse(parser: Parser, left: Expression, token: Token): Expression {
        val right = parser.expression()
        parser.consume(TokenType.RBRACK, "Expected ']' after index access.")
        return IndexAccessExpression(left.position, left, right)
    }

    override val precedence: Int
        get() = Precedence.POSTFIX
}
