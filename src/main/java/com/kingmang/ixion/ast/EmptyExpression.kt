package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class EmptyExpression(pos: Position?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitEmpty(this)
    }
}