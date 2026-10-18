package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class CallExpression(
    pos: Position?,
    val item: Expression,
    val arguments: MutableList<Expression>
) : Expression(pos) {
    var foreign: Token? = null
    var specialization: Map<String, com.kingmang.ixion.runtime.IxType> = emptyMap()

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitCall(this)
    }
}