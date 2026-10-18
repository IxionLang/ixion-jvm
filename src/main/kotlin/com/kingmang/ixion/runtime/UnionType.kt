package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes
import java.util.Collections

/** Immutable alternatives: resolution replaces a union instead of mutating a set key. */
class UnionType(types: Collection<IxType?>) : IxType {
    val types: Set<IxType>

    init {
        val alternatives = linkedSetOf<IxType>()
        for (type in types) {
            requireNotNull(type) { "A union alternative cannot be null" }
            if (type is UnionType) alternatives.addAll(type.types) else alternatives.add(type)
        }
        require(alternatives.isNotEmpty()) { "A union requires at least one alternative" }
        this.types = Collections.unmodifiableSet(alternatives)
    }

    override val defaultValue: Any? get() = null
    override val descriptor: String get() = "Ljava/lang/Object;"
    override val internalName: String? get() = null
    override val loadVariableOpcode: Int get() = Opcodes.ALOAD
    override val name: String get() = types.joinToString(" | ") { it.name ?: it.toString() }
    override val returnOpcode: Int get() = Opcodes.ARETURN
    override val typeClass: Class<*>? get() = null
    override val isNumeric: Boolean get() = false
    override fun kind(): String = "union"
    override fun toString(): String = name
    override fun equals(other: Any?): Boolean = other is UnionType && types == other.types
    override fun hashCode(): Int = types.hashCode()

    companion object {
        fun of(types: Collection<IxType?>): IxType {
            val flattened = types.flatMap { if (it is UnionType) it.types else listOf(requireNotNull(it)) }
                .filter { it !== NeverType }.toSet()
            return when {
                flattened.any { it === BuiltInType.ANY || it is ExternalType && it.foundClass == Any::class.java } -> BuiltInType.ANY
                flattened.isEmpty() -> NeverType
                flattened.size == 1 -> flattened.single()
                else -> UnionType(flattened)
            }
        }
    }
}
