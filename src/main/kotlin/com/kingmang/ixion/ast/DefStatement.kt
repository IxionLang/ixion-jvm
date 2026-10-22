package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.PublicAccess
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.runtime.DefType

class DefStatement(
    pos: Position?,
    val name: Token,
    val parameters: MutableList<ParameterStatement>?,
    val returnType: TypeStatement?,
    val body: BlockStatement?,
    val generics: MutableList<Token>?
) : Statement(pos), PublicAccess {

    /** Resolved signature assigned by declaration collection. */
    var functionType: DefType? = null

    override fun <R> accept(visitor: StatementVisitor<R>): R {
        return visitor.visitFunctionStmt(this)
    }

    override fun identifier(): String {
        return name.source
    }
}
