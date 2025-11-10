package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.PublicAccess
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class StructStatement(
    pos: Position?,
    @JvmField val name: Token,
    @JvmField val fields: MutableList<ParameterStatement?>?,
    @JvmField val generics: MutableList<Token?>?
) : Statement(pos), PublicAccess {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitStruct(this)
    }

    override fun identifier(): String? {
        return name.source
    }
}