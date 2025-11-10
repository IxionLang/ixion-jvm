package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.runtime.IxType

class PropertyAccessExpression(
    pos: Position?,
    @JvmField val expression: Expression?,
    @JvmField val identifiers: MutableList<IdentifierExpression?>?
) : Expression(pos) {
    @JvmField
    var typeChain: MutableList<IxType?> = ArrayList<IxType?>()

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitPropertyAccess(this)
    }
}