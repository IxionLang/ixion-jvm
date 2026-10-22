package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.typechecker.TypeSyntax

/** The native wire format uses Core data only; no HttpExchange escapes into Ixion. */
internal object HttpServerSignatures {
    val pair = TupleType(listOf(BuiltInType.STRING, BuiltInType.STRING))
    val pairs = ListType(pair)
    val request = TupleType(listOf(BuiltInType.STRING, BuiltInType.STRING, pairs, pairs, BuiltInType.STRING))
    val response = TupleType(listOf(BuiltInType.INT, pairs, BuiltInType.STRING))
    private val result = PreludeTypes.declaration("Result", listOf("T", "E")) { p ->
        listOf(AdtVariant("Ok", listOf("value" to p[0])), AdtVariant("Err", listOf("error" to p[1])))
    }.applied(listOf(UnitType, BuiltInType.STRING))

    fun apply(function: DefType) {
        check(function.name == "serve") { "Missing HTTP server signature: ${function.name}" }
        function.parameters.clear()
        function.parameters.addAll(listOf(
            "host" to BuiltInType.STRING,
            "port" to BuiltInType.INT,
            "maxBodyBytes" to BuiltInType.INT,
            "workers" to BuiltInType.INT,
            "queueCapacity" to BuiltInType.INT,
            "shutdownSeconds" to BuiltInType.INT,
            "handler" to LambdaType(arrayListOf("request" to request), response, TypeSyntax.interfaceFor(1))
        ))
        function.returnType = result
    }
}
