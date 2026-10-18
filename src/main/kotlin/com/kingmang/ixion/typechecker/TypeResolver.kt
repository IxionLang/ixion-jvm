package com.kingmang.ixion.typechecker

import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper

object TypeResolver {
    fun getValueFromString(value: String, t: BuiltInType): Any? {
        val result: Any?
        when (t) {
            BuiltInType.CHAR -> result = value.single()
            BuiltInType.BOOLEAN -> result = value.toBoolean()
            BuiltInType.INT -> result = value.toInt()
            BuiltInType.FLOAT -> result = value.toFloat()
            BuiltInType.DOUBLE -> result = value.toDouble()
            // LexerImpl already removed delimiters and decoded escapes.
            BuiltInType.STRING -> result = value

            else -> throw AssertionError("Objects not yet implemented!")
        }
        return result
    }

    @JvmStatic
    fun typesMatch(par: IxType, arg: IxType): Boolean {
        // Reflection compatibility belongs to the external boundary, not language types.
        if (par is ExternalType && arg is ExternalType) {
            val expected = par.foundClass ?: return false
            val actual = arg.foundClass ?: return false
            return expected.isAssignableFrom(actual)
        }
        if (par is ExternalType && arg is LambdaType) {
            return par.foundClass?.isAssignableFrom(arg.functionalInterface) == true
        }
        // Lists cross native boundaries only as the immutable wrapper or an opaque Object.
        if (par is ExternalType && arg is ListType) {
            return par.foundClass == IxListWrapper::class.java ||
                par.foundClass == Any::class.java
        }
        if (par is ExternalType && par.foundClass == Any::class.java) return arg !== BuiltInType.VOID
        return com.kingmang.ixion.types.TypeRelations.isAssignable(SemanticTypes.of(par), SemanticTypes.of(arg))
    }
}
