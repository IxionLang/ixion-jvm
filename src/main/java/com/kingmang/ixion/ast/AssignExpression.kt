package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class AssignExpression(pos: Position?, @JvmField val left: Expression?, @JvmField val right: Expression?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitAssignExpr(this)
    }
}