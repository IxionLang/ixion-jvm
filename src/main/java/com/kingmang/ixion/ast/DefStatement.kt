package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.PublicAccess
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class DefStatement(
    pos: Position?,
    @JvmField val name: Token,
    @JvmField val parameters: MutableList<ParameterStatement?>?,
    @JvmField val returnType: TypeStatement?,
    @JvmField val body: BlockStatement?,
    @JvmField val generics: MutableList<Token?>?
) : Statement(pos), PublicAccess {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitFunctionStmt(this)
    }

    override fun identifier(): String? {
        return name.source
    }
}