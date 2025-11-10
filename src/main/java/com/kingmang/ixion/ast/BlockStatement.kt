package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.api.Context
import com.kingmang.ixion.lexer.Position

class BlockStatement(pos: Position?, @JvmField val statements: MutableList<Statement?>?, @JvmField val context: Context?) : Statement(pos) {
    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitBlockStmt(this)
    }
}