package com.kingmang.ixion.runtime

/** Remap only widening numeric alternatives; unchanged alternatives retain their identity. */
object UnionCoercions {
    @JvmStatic
    fun convert(value: Any, mapping: String): Any {
        val source = when (value) {
            is Char -> "C"
            is Int -> "I"
            is Float -> "F"
            else -> return value
        }
        val target = mapping.split(';').firstOrNull { it.startsWith("$source:") }?.substringAfter(':') ?: return value
        val number = if (value is Char) value.code else value as Number
        return when (target) {
            "I" -> number.toInt()
            "F" -> number.toFloat()
            "D" -> number.toDouble()
            else -> error("Unsupported union conversion $source -> $target")
        }
    }
}
