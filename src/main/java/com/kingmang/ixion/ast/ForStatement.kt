package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token

class ForStatement(pos: Position?, @JvmField val name: Token?, @JvmField val expression: Expression?, @JvmField val block: BlockStatement?) :
    Statement(pos) {
    var localNameIndex: Int = -1
    var localExprIndex: Int = -1

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitFor(this)
    }
}