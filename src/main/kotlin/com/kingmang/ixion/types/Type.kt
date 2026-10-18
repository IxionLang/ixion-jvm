package com.kingmang.ixion.types

import java.util.Collections

/** Semantic types deliberately contain no JVM classes, descriptors or opcodes. */
sealed interface Type

enum class PrimitiveType : Type {
    BOOL, CHAR, INT, FLOAT, DOUBLE, STRING, UNIT, NEVER, ANY
}

enum class DeclarationKind { STRUCT, ADT, FUNCTION }

data class DeclarationId(val module: String, val name: String, val kind: DeclarationKind)

class TypeParameter(val owner: DeclarationId, val index: Int, val name: String) : Type {
    override fun equals(other: Any?) = other is TypeParameter && owner == other.owner && index == other.index
    override fun hashCode() = 31 * owner.hashCode() + index
    override fun toString(): String = name
}

/** Arguments are copied so a semantic type remains a stable map/set key. */
class AppliedType(val declaration: DeclarationId, arguments: List<Type> = emptyList()) : Type {
    val arguments: List<Type> = immutableList(arguments)

    override fun equals(other: Any?) = other is AppliedType && declaration == other.declaration && arguments == other.arguments
    override fun hashCode() = 31 * declaration.hashCode() + arguments.hashCode()
    override fun toString() = "${declaration.module}::${declaration.name}" +
        if (arguments.isEmpty()) "" else arguments.joinToString(", ", "[", "]")
}

data class ListType(val element: Type) : Type

class TupleType(elements: List<Type>) : Type {
    init { require(elements.isNotEmpty()) { "Use unit for the empty product" } }
    val elements: List<Type> = immutableList(elements)

    override fun equals(other: Any?) = other is TupleType && elements == other.elements
    override fun hashCode() = elements.hashCode()
    override fun toString() = elements.joinToString(", ", "(", ")")
}

class FunctionType(parameters: List<Type>, val result: Type) : Type {
    val parameters: List<Type> = immutableList(parameters)

    override fun equals(other: Any?) = other is FunctionType && parameters == other.parameters && result == other.result
    override fun hashCode() = 31 * parameters.hashCode() + result.hashCode()
    override fun toString() = parameters.joinToString(", ", "(", ") => $result")
}

/** Only [of] constructs unions: order, nesting and redundant alternatives disappear. */
class UnionType private constructor(alternatives: Set<Type>) : Type {
    val alternatives: Set<Type> = Collections.unmodifiableSet(LinkedHashSet(alternatives))

    override fun equals(other: Any?) = other is UnionType && alternatives == other.alternatives
    override fun hashCode() = alternatives.hashCode()
    override fun toString() = alternatives.joinToString(" | ")

    companion object {
        fun of(types: Iterable<Type>): Type {
            val flattened = linkedSetOf<Type>()
            for (type in types) {
                when (type) {
                    PrimitiveType.ANY -> return PrimitiveType.ANY
                    PrimitiveType.NEVER -> Unit
                    is UnionType -> flattened.addAll(type.alternatives)
                    else -> flattened.add(type)
                }
            }
            return when (flattened.size) {
                0 -> PrimitiveType.NEVER
                1 -> flattened.single()
                else -> UnionType(flattened)
            }
        }
    }
}

/** External types have semantic names; reflection is confined to the JVM adapter. */
data class ExternalType(val qualifiedName: String) : Type

/** Unresolved references never compare as an inferred top type. */
data class UnresolvedType(val name: String) : Type

private fun <T> immutableList(values: Collection<T>): List<T> =
    Collections.unmodifiableList(ArrayList(values))
