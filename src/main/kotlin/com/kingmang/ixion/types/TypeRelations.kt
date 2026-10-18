package com.kingmang.ixion.types

/** Directed language-level relations. JVM representation is not evidence of assignability. */
object TypeRelations {
    private val numericOrder = listOf(PrimitiveType.CHAR, PrimitiveType.INT, PrimitiveType.FLOAT, PrimitiveType.DOUBLE)

    fun isAssignable(expected: Type, actual: Type): Boolean {
        if (!isResolved(expected) || !isResolved(actual)) return false
        if (actual == PrimitiveType.NEVER) return true
        if (expected == actual) return true
        if (expected == PrimitiveType.ANY) return true
        if (actual == PrimitiveType.ANY) return false
        if (actual is UnionType) return actual.alternatives.all { isAssignable(expected, it) }
        if (expected is UnionType) return injectionTarget(expected, actual) != null
        return canWiden(expected, actual)
    }

    private fun isResolved(type: Type): Boolean = when (type) {
        is UnresolvedType -> false
        is AppliedType -> type.arguments.all(::isResolved)
        is ListType -> isResolved(type.element)
        is TupleType -> type.elements.all(::isResolved)
        is FunctionType -> type.parameters.all(::isResolved) && isResolved(type.result)
        is UnionType -> type.alternatives.all(::isResolved)
        else -> true
    }

    fun canWiden(expected: Type, actual: Type): Boolean {
        val from = numericOrder.indexOf(actual)
        val to = numericOrder.indexOf(expected)
        return from >= 0 && to >= from
    }

    /** Exact matches win; two widening candidates are ambiguous, even if one is narrower. */
    fun injectionTarget(expected: UnionType, actual: Type): Type? {
        if (actual in expected.alternatives) return actual
        return expected.alternatives.filter { isAssignable(it, actual) }.singleOrNull()
    }

    fun commonType(types: Iterable<Type>): Type {
        val values = types.filter { it != PrimitiveType.NEVER }.distinct()
        if (values.isEmpty()) return PrimitiveType.NEVER
        if (values.size == 1) return values.single()
        if (values.all { it in numericOrder }) return numericOrder[values.maxOf { numericOrder.indexOf(it) }]
        return UnionType.of(values)
    }

    fun substitute(type: Type, arguments: Map<TypeParameter, Type>): Type = when (type) {
        is TypeParameter -> arguments[type] ?: type
        is AppliedType -> AppliedType(type.declaration, type.arguments.map { substitute(it, arguments) })
        is ListType -> ListType(substitute(type.element, arguments))
        is TupleType -> TupleType(type.elements.map { substitute(it, arguments) })
        is FunctionType -> FunctionType(type.parameters.map { substitute(it, arguments) }, substitute(type.result, arguments))
        is UnionType -> UnionType.of(type.alternatives.map { substitute(it, arguments) })
        else -> type
    }
}
