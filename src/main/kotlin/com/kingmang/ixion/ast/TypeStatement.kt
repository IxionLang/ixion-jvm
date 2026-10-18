package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.lexer.TokenType
import java.util.*

open class TypeStatement(
    pos: Position?,
    val identifier: Token?,
    val next: Optional<TypeStatement>,
    val listType: Boolean,
    val arguments: List<TypeStatement> = emptyList(),
    val form: TypeForm = TypeForm.NAME,
    val components: List<TypeStatement> = emptyList(),
    val result: TypeStatement? = null
) : Statement(pos) {
    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitTypeAlias(this)
    }

    companion object {
        fun voidInstance(loc: Position): TypeStatement {
            return TypeStatement(
                loc,
                Token(TokenType.TYPEALIAS, loc.line, loc.col, "void"),
                Optional.empty<TypeStatement>(),
                false
            )
        }
    }
}
enum class TypeForm { NAME, LIST, TUPLE, FUNCTION }
