
package com.kingmang.ixion.modules

@Suppress("unused")
object StringModule {

    @JvmStatic
    fun split(s: String, delimiter: String): com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper {
        require(delimiter.isNotEmpty()) { "split delimiter must not be empty" }
        return com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper(
            s.split(delimiter), "string"
        )
    }

    @JvmStatic
    fun startsWith(s: String, prefix: String): Boolean = s.startsWith(prefix)

    @JvmStatic
    fun substring(s: String, start: Int, end: Int): String = s.substring(start, end)

    @JvmStatic
    fun toUpperCase(s: String): String = s.uppercase()

    @JvmStatic
    fun toLowerCase(s: String): String = s.lowercase()

    @JvmStatic
    fun toTitleCase(s: String): String {
        val words = s.split(" ")
        val sb = StringBuilder()
        for ((i, word) in words.withIndex()) {
            if (i > 0) sb.append(" ")
            if (word.isNotEmpty()) {
                sb.append(word[0].uppercaseChar())
                if (word.length > 1) {
                    sb.append(word.substring(1).lowercase())
                }
            }
        }
        return sb.toString()
    }

    @JvmStatic
    fun equals(a: String, b: String): Boolean = a == b
}
