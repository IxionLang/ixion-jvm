package com.kingmang.ixion.runtime

data class GenericType(
    val key: String,
    val owner: com.kingmang.ixion.types.DeclarationId = com.kingmang.ixion.types.DeclarationId(
        "", "<unscoped>", com.kingmang.ixion.types.DeclarationKind.FUNCTION
    ),
    val index: Int = 0
) : IxType {
    override val defaultValue: Any?
        get() = null

    override val descriptor: String
        get() = "Ljava/lang/Object;"

    override val internalName: String
        get() = key

    override val loadVariableOpcode: Int
        get() = org.objectweb.asm.Opcodes.ALOAD

    override val name: String?
        get() = key

    override val returnOpcode: Int
        get() = org.objectweb.asm.Opcodes.ARETURN

    override val typeClass: Class<*>
        get() = Any::class.java

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String? {
        return null
    }
}
