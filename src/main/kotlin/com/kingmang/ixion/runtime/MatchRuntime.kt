package com.kingmang.ixion.runtime

import com.kingmang.ixion.matching.*
import java.util.concurrent.ConcurrentHashMap

object MatchRuntime {
    private val plans = ConcurrentHashMap<String, PatternTest>()
    @JvmStatic fun matches(value: Any?, encoded: String, tags: Array<String>): Boolean =
        test(value, plans.computeIfAbsent(encoded, PatternCodec::decode), tags)

    private fun test(value: Any?, plan: PatternTest, tags: Array<String>): Boolean = when (plan) {
        PatternTest.AnyValue -> true
        is PatternTest.TypeTag -> tags[plan.index] == "any" || typeTag(value) == tags[plan.index]
        is PatternTest.Literal -> when (plan.kind) {
            "int" -> value is Int && value == plan.text.toInt()
            "float" -> value is Float && value == plan.text.toFloat()
            "double" -> value is Double && value == plan.text.toDouble()
            "bool" -> value is Boolean && value == plan.text.toBoolean()
            "char" -> value is Char && value.toString() == plan.text
            "string" -> value is String && value == plan.text
            "unit" -> value === UnitValue
            else -> false
        }
        is PatternTest.Product -> value is IxDataValue &&
            (plan.tag < 0 || value.ixType() == tags[plan.tag]) &&
            (plan.kind != "adt" || value is AdtValue && value.variant == plan.variant) &&
            (plan.kind != "tuple" || value is TupleValue) &&
            plan.fields.indices.all { test(value.ixField(it), plan.fields[it], tags) }
        is PatternTest.Sequence -> {
            val list = value as? CollectionUtil.IxListWrapper
            list != null && (if (plan.rest == null) list.size() == plan.elements.size else list.size() >= plan.elements.size) &&
                plan.elements.indices.all { test(list.element(it), plan.elements[it], tags) } &&
                (plan.rest == null || test(tail(value, plan.elements.size), plan.rest, tags))
        }
    }

    @JvmStatic fun extract(value: Any?, path: String): Any? {
        var result = value
        if (path.isEmpty()) return result
        for (part in path.split('.')) {
            val index = part.substring(1).toInt()
            result = when (part.first()) {
                'f' -> (result as IxDataValue).ixField(index)
                'i' -> (result as CollectionUtil.IxListWrapper).element(index)
                'r' -> tail(result, index)
                else -> error("Invalid extraction path")
            }
        }
        return result
    }
    private fun tail(value: Any?, index: Int): Any = when (value) {
        is CollectionUtil.IxListWrapper -> value.tail(index)
        else -> error("Not a list")
    }
    @JvmStatic fun typeTag(value: Any?): String = when (value) {
        is IxDataValue -> value.ixType()
        is Int -> "int"; is Char -> "char"; is Float -> "float"; is Double -> "double"
        is Boolean -> "bool"; is String -> "string"; UnitValue -> "unit"
        is CollectionUtil.IxListWrapper -> value.name.lowercase().let { if (value.name in setOf("INT", "FLOAT", "DOUBLE", "CHAR", "BOOLEAN", "STRING")) it.replace("boolean", "bool") else value.name } + "[]"
        else -> if (value == null) "<null>" else FunctionTags.signature(value) ?: value.javaClass.name
    }
}
