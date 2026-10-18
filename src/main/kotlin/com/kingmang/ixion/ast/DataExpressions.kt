package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.api.Context
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.matching.PatternPlan

class UnitExpression(pos: Position) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitUnit(this)
}
class TupleExpression(pos: Position, val elements: MutableList<Expression>) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitTuple(this)
}
class TypeApplicationExpression(pos: Position, val name: Token, val types: List<TypeStatement>) : Expression(pos) {
    var resolvedTypes: List<com.kingmang.ixion.runtime.IxType> = emptyList()
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitTypeApplication(this)
}
class UpdateExpression(pos: Position, val base: Expression, val fields: List<Pair<Token, Expression>>) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitUpdate(this)
}
class BlockExpression(pos: Position, val block: BlockStatement, val result: Expression?) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitBlockExpression(this)
}
class CaseExpression(pos: Position, val subject: Expression, val branches: List<CaseBranch>) : Expression(pos) {
    override fun <R> accept(visitor: ExprVisitor<R>): R = visitor.visitCaseExpression(this)
}
class CaseBranch(val pattern: Pattern, val guard: Expression?, val body: BlockExpression) {
    var plan: PatternPlan? = null
    val context: Context get() = body.block.context
}
