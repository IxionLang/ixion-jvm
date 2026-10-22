package com.kingmang.ixion.api

import com.kingmang.ixion.api.IxionConstant.Mutability
import com.kingmang.ixion.exception.RedeclarationException
import com.kingmang.ixion.parser.Node
import com.kingmang.ixion.runtime.IxType
import org.apache.commons.collections4.map.LinkedMap
import java.io.File

class Context {
    private val variables = LinkedMap<String?, IxType?>()
    private val functions = LinkedMap<String, MutableList<com.kingmang.ixion.runtime.DefType>>()
    private val types = LinkedMap<String, IxType>()
    private val mutability: MutableMap<String?, Mutability?> = HashMap()

    @JvmField
    var parent: Context? = null

    fun addVariable(name: String, type: IxType?) {
        variables[name] = type
        mutability[name] = Mutability.IMMUTABLE
    }

    fun addFunction(name: String, type: com.kingmang.ixion.runtime.DefType) {
        functions.getOrPut(name) { arrayListOf() }.add(type)
    }

    fun getFunctions(name: String): List<com.kingmang.ixion.runtime.DefType> =
        functions[name] ?: parent?.getFunctions(name).orEmpty()

    fun getFunction(name: String): com.kingmang.ixion.runtime.DefType? =
        getFunctions(name).singleOrNull()

    fun hasLocalFunctions(name: String): Boolean = functions.containsKey(name)

    fun localFunctions(): Map<String, List<com.kingmang.ixion.runtime.DefType>> = functions

    fun addVariableOrError(ixApi: IxApi, name: String, type: IxType?, file: File, node: Node) {
        if (getVariable(name) != null || getFunctions(name).isNotEmpty()) {
            RedeclarationException().send(ixApi, file, node, name)
        } else {
            addVariable(name, type)
        }
    }

    fun addType(name: String, type: IxType) {
        types[name] = type
    }

    fun getType(name: String): IxType? = types[name] ?: parent?.getType(name)

    fun hasLocalVariable(name: String): Boolean = variables.containsKey(name)

    fun getVariable(name: String): IxType? {
        return when {
            variables.containsKey(name) -> variables[name]
            functions[name]?.size == 1 -> functions[name]?.single()
            parent != null -> parent!!.getVariable(name)
            else -> null
        }
    }

    fun getVariableMutability(name: String): Mutability? {
        return when {
            mutability[name] != null -> mutability[name]
            parent != null -> parent!!.getVariableMutability(name)
            else -> null
        }
    }

    inline fun <reified T> getVariableTyped(name: String): T? {
        val v = getVariable(name)
        if (T::class.isInstance(v))
            return v as T
        return null
    }

    fun setVariableMutability(name: String, m: Mutability?) {
        if (mutability[name] != null) {
            mutability[name] = m
        }
    }

    fun setVariableType(name: String, type: IxType?) {
        if (variables.containsKey(name)) {
            variables[name] = type
        } else if (parent != null) {
            parent!!.setVariableType(name, type)
        }
    }
}
