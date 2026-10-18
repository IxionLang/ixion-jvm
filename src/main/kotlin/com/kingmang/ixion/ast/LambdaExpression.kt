package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class LambdaExpression(
    pos: Position?,
    val parameters: MutableList<ParameterStatement>,
    val returnType: TypeStatement,
    val body: BlockStatement,
    val inferredReturn: Boolean = false
) : Expression(pos) {
    var captures: List<Pair<String, com.kingmang.ixion.runtime.IxType>> = emptyList()
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLambda(this)
    }
}