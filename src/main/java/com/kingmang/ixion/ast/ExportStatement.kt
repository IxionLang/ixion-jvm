package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position

// export statement = pub (public)
class ExportStatement(pos: Position?, @JvmField val stmt: Statement?) : Statement(pos) {
    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitExport(this)
    }
}