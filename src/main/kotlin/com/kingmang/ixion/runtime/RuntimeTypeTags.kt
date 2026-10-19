package com.kingmang.ixion.runtime

object RuntimeTypeTags {
    @JvmStatic
    fun union(types: Array<String>): String {
        fun alternatives(value: String): List<String> {
            if (!value.startsWith("union(") || !value.endsWith(')')) return listOf(value)
            val inner = value.substring(6, value.lastIndex)
            val parts = arrayListOf<String>()
            var depth = 0
            var start = 0
            for (index in inner.indices) when (inner[index]) {
                '[', '(' -> depth++
                ']', ')' -> depth--
                ',' -> if (depth == 0) {
                    parts.add(inner.substring(start, index)); start = index + 1
                }
            }
            parts.add(inner.substring(start))
            return parts.flatMap(::alternatives)
        }

        val flattened = types.flatMap(::alternatives).filter { it != "never" }.distinct().sorted()
        return when {
            "any" in flattened -> "any"
            flattened.isEmpty() -> "never"
            flattened.size == 1 -> flattened.single()
            else -> flattened.joinToString(",", "union(", ")")
        }
    }
}
