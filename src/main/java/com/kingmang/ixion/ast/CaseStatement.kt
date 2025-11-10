package com.kingmang.ixion.ast

import com.kingmang.ixion.StatementVisitor
import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.runtime.IxType
import org.javatuples.Pair

class CaseStatement(
    pos: Position?,
    @JvmField val expression: Expression?,
    @JvmField val cases: MutableMap<TypeStatement?, Pair<String?, BlockStatement?>?>?
) : Statement(pos) {
    @JvmField
    val types: MutableMap<TypeStatement?, IxType?> = HashMap<TypeStatement?, IxType?>()

    override fun <R> accept(visitor: StatementVisitor<R?>?): R? {
        return visitor?.visitMatch(this)
    }
}