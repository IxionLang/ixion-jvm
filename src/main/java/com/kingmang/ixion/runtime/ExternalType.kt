package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes

@JvmRecord
data class ExternalType(@JvmField val foundClass: Class<*>?) : IxType {
    override fun getDefaultValue(): Any? {
        return null
    }

    override fun getDescriptor(): String {
        return foundClass!!.descriptorString()
    }


    override fun getInternalName(): String {
        return getName().replace(".", "/")
    }

    override fun getLoadVariableOpcode(): Int {
        return Opcodes.ALOAD
    }


    override fun getName(): String {
        return foundClass!!.getName()
    }


    override fun getReturnOpcode(): Int {
        return Opcodes.ARETURN
    }


    override fun getTypeClass(): Class<*>? {
        return foundClass
    }

    override fun isNumeric(): Boolean {
        return false
    }

    override fun kind(): String? {
        return null
    }

    override fun toString(): String {
        return getName()
    }
}
