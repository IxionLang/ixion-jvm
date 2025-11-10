package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.PublicAccess
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class TypeAliasStatement(pos: Position?, @JvmField val identifier: Token, @JvmField val typeStmt: TypeStatement?) : Statement(pos),
    PublicAccess {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitTypeAlias(this)
    }

    override fun identifier(): String? {
        return identifier.source
    }
}