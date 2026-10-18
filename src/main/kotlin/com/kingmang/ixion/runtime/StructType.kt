package com.kingmang.ixion.runtime

import com.kingmang.ixion.types.DeclarationId
import com.kingmang.ixion.types.DeclarationKind
import org.objectweb.asm.Opcodes
import java.io.Serializable

open class StructType(
    override var name: String,
    open val parameters: MutableList<Pair<String, IxType>>,
    open val generics: MutableList<String>,
    val declarationId: DeclarationId = DeclarationId("", name, DeclarationKind.STRUCT)
) : IxType, Serializable {
    open var qualifiedName: String? = null
    open var parentName: String? = null

    override fun equals(other: Any?): Boolean =
        other is StructType && other !is AppliedStructType && this !is AppliedStructType && declarationId == other.declarationId

    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = "L${qualifiedName!!.replace(":", "/")};"

    override val internalName: String?
        get() = name.replace(".", "/")

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>?
        get() = null

    fun hasGenerics(): Boolean {
        return !generics.isEmpty()
    }

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String {
        return "struct"
    }

    // Printing a recursive declaration must not expand its fields recursively.
    override fun toString(): String =
        (if (declarationId.module.isEmpty()) "" else "${declarationId.module}::") + declarationId.name

    override fun hashCode(): Int = declarationId.hashCode()
}
