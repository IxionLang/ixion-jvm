package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import java.util.*

open class TypeStatement(
    pos: Position?,
    @JvmField val identifier: Token?,
    @JvmField val next: Optional<TypeStatement?>?,
    @JvmField val listType: Boolean
) : Statement(pos) {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitTypeAlias(this)
    }

    companion object {
        fun voidInstance(loc: Position): TypeStatement {
            return TypeStatement(
                loc,
                Token(TokenType.TYPEALIAS, loc.line, loc.col, "void"),
                Optional.empty<TypeStatement?>() as Optional<TypeStatement?>?,
                false
            )
        }
    }
}