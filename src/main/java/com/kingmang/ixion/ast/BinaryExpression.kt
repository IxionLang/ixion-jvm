package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class BinaryExpression(pos: Position?, @JvmField val left: Expression?, @JvmField val operator: Token?, @JvmField val right: Expression?) :
    Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitBinaryExpr(this)
    }
}