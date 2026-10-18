package com.kingmang.ixion.typechecker

import com.kingmang.ixion.lexer.TokenType
import com.kingmang.ixion.runtime.BuiltInType
import com.kingmang.ixion.runtime.IxType
import java.util.stream.Collectors

object TypeUtils {
    fun parameterString(parameters: MutableList<Pair<String, IxType>>): String {
        return parameters
            .stream()
            .map { "${it!!.first} ${it.second}" }
            .collect(Collectors.joining(", "))
    }

    fun convert(c: Class<*>?): Class<*>? {
        return when (c) {
            Int::class.java     -> Int::class.javaPrimitiveType
            Float::class.java   -> Float::class.javaPrimitiveType
            Boolean::class.java -> Boolean::class.javaPrimitiveType
            else                -> null
        }
    }

    fun getMethodDescriptor(parameters: MutableCollection<Pair<String?, IxType?>?>, returnType: IxType): String {
        return com.kingmang.ixion.codegen.JvmTypeLayout.methodDescriptor(
            parameters.map { SemanticTypes.of(it!!.second!!) }, SemanticTypes.of(returnType)
        )
    }

    fun getFromToken(tokenType: TokenType): BuiltInType {
        return when (tokenType) {
            TokenType.TRUE, TokenType.FALSE -> BuiltInType.BOOLEAN
            TokenType.STRING                -> BuiltInType.STRING
            TokenType.CHAR                  -> BuiltInType.CHAR
            TokenType.INT                   -> BuiltInType.INT
            TokenType.FLOAT                 -> BuiltInType.FLOAT
            TokenType.DOUBLE                -> BuiltInType.DOUBLE
            else                            -> throw IllegalStateException("Unexpected value: $tokenType")
        }
    }

    fun getFromString(value: String): BuiltInType? {
        return when (value) {
            "int"       -> BuiltInType.INT
            "char"      -> BuiltInType.CHAR
            "float"     -> BuiltInType.FLOAT
            "double"    -> BuiltInType.DOUBLE
            "bool"      -> BuiltInType.BOOLEAN
            "string"    -> BuiltInType.STRING
            "any"       -> BuiltInType.ANY
            "void"      -> BuiltInType.VOID
            else        -> null
        }
    }
}
