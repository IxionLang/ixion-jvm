package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes
import java.util.*

data class ListType(val contentType: IxType) : IxType {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val listType = other as ListType
        return contentType == listType.contentType
    }


    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = "Lcom/kingmang/ixion/runtime/CollectionUtil\$IxListWrapper;"

    override val internalName: String?
        get() = null

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val name: String
        get() = "$contentType[]"

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>
        get() = CollectionUtil.IxListWrapper::class.java

    override fun hashCode(): Int {
        return Objects.hash(contentType)
    }

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String {
        return "list"
    }

    override fun toString(): String {
        return name
    }
}
