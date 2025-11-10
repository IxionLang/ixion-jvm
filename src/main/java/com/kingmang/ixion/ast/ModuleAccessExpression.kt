package com.kingmang.ixion.ast

import com.kingmang.ixion.ExprVisitor
import com.kingmang.ixion.lexer.Position

class ModuleAccessExpression(pos: Position?, @JvmField val identifier: IdentifierExpression?, @JvmField val foreign: Expression?) :
    Expression(pos) {

    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitModuleAccess(this)
    }
}