package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.ast.Statement.TopLevel
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import java.util.*

class UseStatement(pos: Position?, @JvmField val stringLiteral: Token?, val identifier: Optional<Token?>?) : Statement(pos),
    TopLevel {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitUse(this)
    }

    companion object {
        val instance: UseStatement = UseStatement(
            Position(0, 0),
            Token(TokenType.STRING, 0, 0, "prelude"),
            Optional.empty<Token?>() as Optional<Token?>?
        )
    }
}