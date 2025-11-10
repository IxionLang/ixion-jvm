package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position

class IfStatement(
    pos: Position?,
    @JvmField val condition: Expression?,
    @JvmField val trueBlock: BlockStatement?,
    @JvmField val falseStatement: Statement?
) : Statement(pos) {
    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitIf(this)
    }
}