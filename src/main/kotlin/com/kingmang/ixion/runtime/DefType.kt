package com.kingmang.ixion.runtime

import com.kingmang.ixion.api.IxFile
import com.kingmang.ixion.types.DeclarationId
import com.kingmang.ixion.types.DeclarationKind
import com.kingmang.ixion.typechecker.TypeUtils
import org.objectweb.asm.commons.GeneratorAdapter

class DefType : StructType {
    val localMap: MutableMap<String?, Int?> = HashMap()
    val argMap: MutableMap<String?, Int?> = HashMap()
    override var name: String
    var returnType: IxType = BuiltInType.VOID
    var ga: GeneratorAdapter? = null
    var glue: Boolean = false
    var jvmDescriptor: String? = null
    var reifiedNative: Boolean = false
    var hasReturn2: Boolean = false
    var isPrefixed: Boolean = false
    var owner: String? = null
    var bridgeVoidToObject: Boolean = false
    var reifiedArgumentIndex: Int = -1

    @JvmField
    var external: IxFile? = null

    constructor(
        name: String, parameters: MutableList<Pair<String, IxType>>,
        declarationId: DeclarationId = DeclarationId("", name, DeclarationKind.FUNCTION)
    ) : super(
        name,
        parameters,
        ArrayList<String>(),
        declarationId
    ) {
        this.name = name
    }

    constructor(
        name: String,
        parameters: MutableList<Pair<String, IxType>>,
        generics: MutableList<String>,
        declarationId: DeclarationId = DeclarationId("", name, DeclarationKind.FUNCTION)
    ) : super(name, parameters, generics, declarationId) {
        this.name = name
    }

    override val defaultValue: Any?
        get() = null

    override val descriptor: String?
        get() = null

    override val internalName: String?
        get() = null

    override val loadVariableOpcode: Int
        get() = 0

    override val returnOpcode: Int
        get() = 0

    override val typeClass: Class<*>?
        get() = null

    override val isNumeric: Boolean
        get() = false

    override fun kind(): String {
        return "function"
    }

    override fun toString(): String {
        return "def $name(${TypeUtils.parameterString(parameters)}) $returnType"
    }

}
