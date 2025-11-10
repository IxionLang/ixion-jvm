package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.parser.Node
import com.kingmang.ixion.runtime.IxType
import com.kingmang.ixion.runtime.UnknownType

abstract class Expression(
    val pos: Position?
) : Node {
    var realType: IxType = UnknownType("unknown")

    abstract fun <R> accept(visitor: ExprVisitor<R>): R

    override fun pos(): Position? = pos
}