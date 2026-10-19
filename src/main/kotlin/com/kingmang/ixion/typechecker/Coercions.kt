package com.kingmang.ixion.typechecker

import com.kingmang.ixion.runtime.IxType
import com.kingmang.ixion.runtime.UnionType
import com.kingmang.ixion.types.TypeRelations

/** Shared decisions for conversions at typed boundaries in both JVM backends. */
object Coercions {
    fun unionTarget(expected: UnionType, actual: IxType): IxType {
        val type = SemanticTypes.of(expected)
        val source = SemanticTypes.of(actual)
        val target = if (type is com.kingmang.ixion.types.UnionType) TypeRelations.injectionTarget(type, source)
        else type.takeIf { TypeRelations.isAssignable(it, source) }
        requireNotNull(target) { "No unique union injection from $actual to $expected" }
        return expected.types.first { SemanticTypes.of(it) == target }
    }

    /** Numeric remapping is necessary when a union value crosses into a wider union. */
    fun unionMapping(expected: UnionType, actual: UnionType): String {
        if (SemanticTypes.of(expected) == com.kingmang.ixion.types.PrimitiveType.ANY) return ""
        return actual.types.mapNotNull { source ->
            val target = unionTarget(expected, source)
            if (SemanticTypes.of(source) == SemanticTypes.of(target)) null
            else "${source.descriptor}:${target.descriptor}"
        }.joinToString(";")
    }
}
