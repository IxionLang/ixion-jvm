package com.kingmang.ixion.typechecker

import com.kingmang.ixion.ast.*
import com.kingmang.ixion.runtime.NeverType

object ControlFlow {
    fun exits(statement: Statement?): Boolean = when (statement) {
        is ReturnStatement -> true
        is BlockStatement -> statement.statements.any(::exits)
        is IfStatement -> exits(statement.trueBlock) && statement.falseStatement != null && exits(statement.falseStatement)
        is ExpressionStatement -> statement.expression.realType === NeverType
        else -> false
    }
}
