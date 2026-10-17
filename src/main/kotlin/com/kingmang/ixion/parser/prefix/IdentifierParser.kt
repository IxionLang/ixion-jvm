package com.kingmang.ixion.parser.prefix

import com.kingmang.ixion.ast.EmptyListExpression
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.ast.IdentifierExpression
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.parser.Parser

class IdentifierParser : PrefixParselet {
    override fun parse(parser: Parser, token: Token): Expression {
        var token = token
        val pos = parser.pos

        if (parser.match(TokenType.MODULE)) {
            val nextToken = parser.consume(TokenType.IDENTIFIER, "Expected identifier after module separator `::`.")
            token = Token(TokenType.IDENTIFIER, pos.line, pos.col, token.source + "::" + nextToken.source)
        }

        if (parser.peek().type == TokenType.LBRACK && parser.lookAhead(1).type == TokenType.RBRACK) {
            parser.consume()
            parser.consume(TokenType.RBRACK, "Expect ']' to close list constructor.")
            return EmptyListExpression(pos, token)
        }

        return IdentifierExpression(pos, token)
    }
}