package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class CallExpression(pos: Position?, @JvmField val item: Expression?, @JvmField val arguments: MutableList<Expression?>?) :
    Expression(pos) {
    var foreign: Token? = null

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitCall(this)
    }
}