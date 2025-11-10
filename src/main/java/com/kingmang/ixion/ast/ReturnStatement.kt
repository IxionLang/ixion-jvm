package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position

class ReturnStatement(pos: Position?, @JvmField val expression: Expression?) : Statement(pos) {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitReturnStmt(this)
    }
}