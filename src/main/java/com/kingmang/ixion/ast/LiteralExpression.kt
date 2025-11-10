package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class LiteralExpression(pos: Position?, @JvmField val literal: Token?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLiteralExpr(this)
    }
}