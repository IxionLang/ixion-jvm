package com.kingmang.ixion.runtime

import com.kingmang.ixion.api.IxFile
import com.kingmang.ixion.ast.Expression
import com.kingmang.ixion.typechecker.TypeUtils
import org.javatuples.Pair
import org.objectweb.asm.commons.GeneratorAdapter

class DefType : StructType {
    val localMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
    val argMap: MutableMap<String?, Int?> = HashMap<String?, Int?>()
    @JvmField
    var name: String
    @JvmField
    var returnType: IxType = BuiltInType.VOID
    var ga: GeneratorAdapter? = null
    @JvmField
    var glue: Boolean = false
    @JvmField
    var hasReturn2: Boolean = false
    @JvmField
    var isPrefixed: Boolean = false
    @JvmField
    var owner: String? = null

    @JvmField
    var specializations: MutableList<MutableMap<String?, IxType?>?> = ArrayList<MutableMap<String?, IxType?>?>()

    var currentSpecialization: MutableMap<String?, IxType?>? = null
    @JvmField
    var external: IxFile? = null

    constructor(name: String, parameters: MutableList<Pair<String?, IxType?>?>?) : super(
        name,
        parameters,
        ArrayList<String?>()
    ) {
        this.name = name
    }

    constructor(
        name: String,
        parameters: MutableList<Pair<String?, IxType?>?>?,
        generics: MutableList<String?>?
    ) : super(name, parameters, generics) {
        this.name = name
    }

    fun buildParametersFromSpecialization(specialization: MutableMap<String?, IxType?>): MutableList<Pair<String?, IxType?>?> {
        val p = ArrayList<Pair<String?, IxType?>?>()
        for (pair in parameters) {
            val pt = pair.getValue1()
            if (pt is GenericType) {
                p.add(pair.setAt1<IxType?>(specialization.get(pt.key)))
            } else {
                p.add(pair)
            }
        }
        return p
    }

    fun buildSpecialization(arguments: MutableList<Expression?>): MutableMap<String?, IxType?> {
        val argTypes = arguments.stream().map<IxType> { ex: Expression? -> ex!!.realType }.toList()
        val specialization = HashMap<String?, IxType?>()
        for (i in parameters.indices) {
            val p = parameters.get(i)
            val pt = p.getValue1()
            if (pt is GenericType) {
                specialization.put(pt.key, argTypes.get(i))
            }
        }
        return specialization
    }

    override fun getDefaultValue(): Any? {
        return null
    }

    override fun getDescriptor(): String? {
        return null
    }

    override fun getInternalName(): String? {
        return null
    }

    override fun getLoadVariableOpcode(): Int {
        return 0
    }

    override fun getName(): String? {
        return null
    }

    override fun getReturnOpcode(): Int {
        return 0
    }

    override fun getTypeClass(): Class<*>? {
        return null
    }

    override fun isNumeric(): Boolean {
        return false
    }

    override fun kind(): String {
        return "function"
    }

    override fun toString(): String {
        return "def " + name + "(" + TypeUtils.parameterString(parameters) + ") " + returnType
    }

    companion object {
        fun getSpecializedType(specialization: MutableMap<String?, IxType?>, key: String?): IxType? {
            return specialization.get(key)
        }
    }
}
