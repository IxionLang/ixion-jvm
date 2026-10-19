package com.kingmang.ixion.runtime

import java.util.stream.Collectors

class MonomorphizedStruct(val struct: StructType) : IxType {
    val resolved: MutableMap<String?, IxType?> = HashMap()

    override val defaultValue: Any?
        get() = struct.defaultValue

    override val descriptor: String?
        get() = struct.descriptor

    override val internalName: String?
        get() = struct.internalName

    override val loadVariableOpcode: Int
        get() = struct.loadVariableOpcode

    override val name: String
        get() = struct.name

    override val returnOpcode: Int
        get() = struct.returnOpcode

    override val typeClass: Class<*>?
        get() = struct.typeClass

    override val isNumeric: Boolean
        get() = struct.isNumeric

    override fun kind(): String {
        return struct.kind()
    }

    override fun toString(): String {
        return "type $name = struct[" +
                struct.generics
                    .stream()
                    .map { g: String? -> g + "=" + resolved[g] }
                    .collect(Collectors.joining(",")) + "]"
    }
}
