package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class LambdaExpression(
    pos: Position?, val parameters: MutableList<ParameterStatement?>?,
    val returnType: TypeStatement?, val body: BlockStatement?
) : Expression(pos) {

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visit(this)
    }
}