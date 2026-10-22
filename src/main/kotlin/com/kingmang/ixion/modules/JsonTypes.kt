package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.types.DeclarationId
import com.kingmang.ixion.types.DeclarationKind

/** A nominal, immutable JSON tree shared by native code and both backends. */
internal object JsonTypes {
    private val declaration = AdtDeclaration(DeclarationId("<json>", "Value", DeclarationKind.ADT), emptyList())
    val value = declaration.applied(emptyList())
    val member = TupleType(listOf(BuiltInType.STRING, value))
    init {
        declaration.variants.addAll(listOf(
            AdtVariant("Null", emptyList()),
            AdtVariant("Bool", listOf("value" to BuiltInType.BOOLEAN)),
            AdtVariant("Number", listOf("literal" to BuiltInType.STRING)),
            AdtVariant("String", listOf("value" to BuiltInType.STRING)),
            AdtVariant("Array", listOf("values" to ListType(value))),
            AdtVariant("Object", listOf("members" to ListType(member)))
        ))
    }

    private val result = PreludeTypes.declaration("Result", listOf("T", "E")) { p ->
        listOf(AdtVariant("Ok", listOf("value" to p[0])), AdtVariant("Err", listOf("error" to p[1])))
    }

    fun signature(function: DefType) {
        val (parameters, returns) = when (function.name) {
            "parse" -> listOf("text" to BuiltInType.STRING) to result.applied(listOf(value, BuiltInType.STRING))
            "stringify" -> listOf("value" to value) to result.applied(listOf(BuiltInType.STRING, BuiltInType.STRING))
            "get" -> listOf("value" to value, "key" to BuiltInType.STRING) to PreludeTypes.option(value)
            else -> error("Missing JSON signature: ${function.name}")
        }
        function.parameters.clear()
        function.parameters.addAll(parameters)
        function.returnType = returns
    }
}
