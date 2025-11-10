package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.parser.Node

abstract class Statement protected constructor(val pos: Position?) : Node {
    abstract fun <R> accept(visitor: StatementVisitor<R?>?): R?

    override fun pos(): Position? {
        return pos
    }

    interface TopLevel
}