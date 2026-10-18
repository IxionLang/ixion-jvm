package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.PublicAccess
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class EnumStatement(
    pos: Position?,
    val name: Token,
    val values: MutableList<Token?>?,
    val generics: List<Token> = emptyList(),
    val variants: List<VariantDeclaration> = values.orEmpty().filterNotNull().map { VariantDeclaration(it, emptyList()) }
) : Statement(pos), PublicAccess {
    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitEnum(this)
    }

    override fun identifier(): String {
        return name.source
    }
}
data class VariantDeclaration(val name: Token, val fields: List<ParameterStatement>)
