package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class EmptyListExpression(pos: Position?, @JvmField val tokenType: Token?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitEmptyList(this)
    }
}