package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class PrefixExpression(pos: Position?, @JvmField val operator: Token?, @JvmField val right: Expression?) : Expression(pos) {

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitPrefix(this)
    }
}