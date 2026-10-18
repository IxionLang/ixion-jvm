package com.kingmang.ixion.runtime

import org.objectweb.asm.Opcodes

data class LambdaType(
    val parameters: MutableList<Pair<String, IxType>>,
    var returnType: IxType,
    val functionalInterface: Class<*>
) : IxType {
    override val defaultValue: Any?
        get() = null

    override val descriptor: String
        get() = functionalInterface.descriptorString()

    override val internalName: String
        get() = functionalInterface.name.replace('.', '/')

    override val loadVariableOpcode: Int
        get() = Opcodes.ALOAD

    override val name: String
        get() = functionalInterface.name

    override val returnOpcode: Int
        get() = Opcodes.ARETURN

    override val typeClass: Class<*>
        get() = functionalInterface

    override val isNumeric: Boolean
        get() = false

    fun erasedApplyDescriptor(): String {
        if (parameters.size > 4) return "([Ljava/lang/Object;)Ljava/lang/Object;"
        val args = "Ljava/lang/Object;".repeat(parameters.size)
        return "($args)Ljava/lang/Object;"
    }

    override fun kind(): String {
        return "lambda"
    }
}
