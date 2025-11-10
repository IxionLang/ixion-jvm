package com.kingmang.ixion.api

import com.kingmang.ixion.api.IxionConstant.Mutability
import com.kingmang.ixion.exception.RedeclarationException
import com.kingmang.ixion.parser.Node
import com.kingmang.ixion.runtime.IxType
import org.apache.commons.collections4.map.LinkedMap
import java.io.File

class Context {
    private val variables = LinkedMap<String?, IxType?>()

    private val mutability: MutableMap<String?, Mutability?> = HashMap()
    @JvmField
    var parent: Context? = null

    fun addVariable(name: String?, type: IxType?) {
        variables[name] = type
        mutability[name] = Mutability.IMMUTABLE
    }

    fun addVariableOrError(ixApi: IxApi?, name: String?, type: IxType?, file: File?, node: Node) {
        if (getVariable(name) != null) {
            RedeclarationException().send(ixApi, file, node, name)
        } else {
            addVariable(name, type)
        }
    }

    fun getVariable(name: String?): IxType? {
        if (variables.get(name) != null) {
            return variables.get(name)
        }
        if (parent != null) {
            return parent!!.getVariable(name)
        }
        return null
    }

    fun getVariableMutability(name: String?): Mutability? {
        if (mutability.get(name) != null) {
            return mutability.get(name)
        }
        if (parent != null) {
            return parent!!.getVariableMutability(name)
        }
        return null
    }

    fun <T> getVariableTyped(name: String?, clazz: Class<T?>): T? {
        val v = getVariable(name)
        if (clazz.isInstance(v)) {
            return v as T
        }
        return null
    }

    fun setVariableMutability(name: String?, m: Mutability?) {
        if (mutability[name] != null) {
            mutability[name] = m
        }
    }

    fun setVariableType(name: String?, type: IxType?) {
        if (variables[name] != null) {
            variables[name] = type
        }
    }
}
