package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class LiteralListExpression(pos: Position?, @JvmField val entries: MutableList<Expression?>?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLiteralList(this)
    }
}