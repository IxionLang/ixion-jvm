package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class BadExpression(pos: Position?, vararg badTokens: Token?) : Expression(pos) {
    val badTokens: Array<Token?>?

    init {
        this.badTokens = badTokens as Array<Token?>?
    }

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitBad(this)
    }
}