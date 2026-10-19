package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes
import java.io.Serializable

class UnknownType : IxType, Serializable {
    val typeName: String

    constructor(typeName: String) {
        this.typeName = typeName
        counter++
    }

    constructor() {
        this.typeName = ""
        counter++
    }

    override val defaultValue: Any?
        get() = null

    override val descriptor: String
        get() = "L$internalName;"

    override val internalName: String?
        get() = name.replace(".", "/")

    override val loadVariableOpcode: Int
        get() = throw RuntimeException("ALOAD on UnknownType")

    override val name: String
        get() = typeName.ifEmpty { "<unknown>" }

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>?
        get() = null

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String? {
        return null
    }

    override fun toString(): String {
        return "$$name"
    }

    companion object {
        var counter: Int = 1
    }
}
