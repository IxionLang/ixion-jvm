package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position

class WhileStatement(pos: Position?, @JvmField val condition: Expression?, @JvmField val block: BlockStatement?) : Statement(pos) {

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitWhile(this)
    }
}