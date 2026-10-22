package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.typechecker.TypeSyntax
import java.lang.reflect.Method

object Modules {
    val modules: MutableMap<String?, Class<*>?> = HashMap()

    init {
        modules["std"] = Prelude::class.java
        modules["http"] = HttpModule::class.java
        modules["async"] = AsyncModule::class.java
        modules["strings"] = StringModule::class.java
        modules["http_server"] = HttpServerModule::class.java
        modules["json"] = JsonModule::class.java
    }

    @JvmStatic
    fun getExports(module: String?): MutableList<DefType?> {
        val result = ArrayList<DefType?>()
        if (modules.containsKey(module)) {
            val c: Class<*> = modules[module]!!
            val m = c.getMethods()
            for (method in m) {
                var name = method.name
                if (method.declaringClass == Any::class.java || method.isSynthetic) {
                    continue
                }
                val parameters = getPairs(method)
                if (module == "std" && name == "panic") {
                    parameters.clear(); parameters.add("message" to BuiltInType.STRING)
                }
                var isPrefixed = false
                if (name.startsWith("_")) {
                    name = name.substring(1)
                    isPrefixed = true
                }
                val funcType = DefType(name, parameters, com.kingmang.ixion.types.DeclarationId(module!!, name, com.kingmang.ixion.types.DeclarationKind.FUNCTION))
                funcType.isPrefixed = isPrefixed
                funcType.returnType = if (module == "std" && name == "panic") com.kingmang.ixion.runtime.NeverType else mapJavaType(method.returnType)

                if (module == "std") listSignature(funcType)
                if (module == "http_server") HttpServerSignatures.apply(funcType)
                if (module == "json") JsonTypes.signature(funcType)
                if (module == "strings" && name == "split") funcType.returnType = ListType(BuiltInType.STRING)
                funcType.glue = true
                funcType.jvmDescriptor = org.objectweb.asm.Type.getMethodDescriptor(method)
                funcType.owner = c.getName().replace('.', '/')
                result.add(funcType)
            }
        }
        return result
    }

    fun getTypes(module: String): Map<String, IxType> =
        if (module == "json") mapOf("Value" to JsonTypes.value) else emptyMap()

    /** Explicit Ixion signatures; reflection supplies only the physical JVM ABI. */
    private fun listSignature(function: DefType) {
        val names = when (function.name) {
            "map", "fold" -> listOf("A", "B")
            "len", "append", "concat", "get", "updated", "filter" -> listOf("T")
            else -> return
        }
        function.generics.addAll(names)
        val types = names.mapIndexed { i, name -> GenericType(name, function.declarationId, i) }
        val a = types[0]
        val list = ListType(a)
        fun callback(parameters: List<IxType>, result: IxType) = LambdaType(
            parameters.mapIndexed { i, type -> "p$i" to type }.toMutableList(), result, TypeSyntax.interfaceFor(parameters.size))
        val parameters: List<Pair<String, IxType>> = when (function.name) {
            "len" -> listOf("values" to list)
            "append" -> listOf("values" to list, "value" to a)
            "concat" -> listOf("left" to list, "right" to list)
            "get" -> listOf("values" to list, "index" to BuiltInType.INT)
            "updated" -> listOf("values" to list, "index" to BuiltInType.INT, "value" to a)
            "map" -> listOf("values" to list, "transform" to callback(listOf(a), types[1]))
            "filter" -> listOf("values" to list, "predicate" to callback(listOf(a), BuiltInType.BOOLEAN))
            "fold" -> listOf("values" to list, "initial" to types[1], "transform" to callback(listOf(types[1], a), types[1]))
            else -> error("Unknown list function")
        }
        function.parameters.clear()
        function.parameters.addAll(parameters)
        function.returnType = when (function.name) {
            "len" -> BuiltInType.INT
            "get" -> PreludeTypes.option(a)
            "updated" -> PreludeTypes.option(list)
            "map" -> ListType(types[1])
            "fold" -> types[1]
            else -> list
        }
        function.reifiedNative = function.name == "map"
    }

    private fun getPairs(method: Method): ArrayList<Pair<String, IxType>> {
        val parameters = ArrayList<Pair<String, IxType>>()
        for (p in method.parameterTypes) {
            parameters.add(Pair("_", mapJavaType(p)))
        }
        return parameters
    }

    private fun mapJavaType(type: Class<*>): IxType {
        return when (type) {
            Integer.TYPE,
            Integer::class.java -> BuiltInType.INT

            java.lang.Float.TYPE,
            java.lang.Float::class.java -> BuiltInType.FLOAT

            java.lang.Double.TYPE,
            java.lang.Double::class.java -> BuiltInType.DOUBLE

            java.lang.Boolean.TYPE,
            java.lang.Boolean::class.java -> BuiltInType.BOOLEAN

            Character.TYPE,
            Character::class.java -> BuiltInType.CHAR

            java.lang.String::class.java -> BuiltInType.STRING

            Void.TYPE,
            Void::class.java -> BuiltInType.VOID

            else -> ExternalType(type)
        }
    }
}
