package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class PostfixExpression(pos: Position?, @JvmField val expression: Expression?, @JvmField val operator: Token?) : Expression(pos) {
    var localIndex: Int = -1

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitPostfixExpr(this)
    }
}