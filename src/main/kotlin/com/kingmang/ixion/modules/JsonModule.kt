package com.kingmang.ixion.modules

import com.kingmang.ixion.runtime.AdtValue
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import com.kingmang.ixion.runtime.TupleValue
import com.kingmang.ixion.typechecker.TypeTags

/** Strict JSON with lossless number literals, bounded nesting and no mutable foreign values. */
@Suppress("unused")
object JsonModule {
    private const val TAG = "<json>::Value"
    private const val MAX_DEPTH = 128
    private val number = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
    private val memberTag = TypeTags.constant(JsonTypes.member)

    @JvmStatic
    fun parse(text: String): AdtValue = try {
        AdtValue("<prelude>::Result[$TAG,string]", "Ok", arrayOf(Parser(text).parse()))
    } catch (error: IllegalArgumentException) {
        AdtValue("<prelude>::Result[$TAG,string]", "Err", arrayOf(error.message ?: "Invalid JSON"))
    }

    @JvmStatic
    fun stringify(value: AdtValue): AdtValue = try {
        val output = StringBuilder()
        write(value, output, 0)
        AdtValue("<prelude>::Result[string,string]", "Ok", arrayOf(output.toString()))
    } catch (error: IllegalArgumentException) {
        AdtValue("<prelude>::Result[string,string]", "Err", arrayOf(error.message ?: "Invalid JSON value"))
    }

    @JvmStatic
    fun get(value: AdtValue, key: String): AdtValue {
        if (value.variant == "Object") for (item in value.ixField(0) as IxListWrapper) {
            val pair = item as TupleValue
            if (pair.ixField(0) == key) return AdtValue("<prelude>::Option[$TAG]", "Some", arrayOf(pair.ixField(1)))
        }
        return AdtValue("<prelude>::Option[$TAG]", "None", emptyArray())
    }

    private fun node(variant: String, vararg fields: Any?) = AdtValue(TAG, variant, arrayOf(*fields))

    private fun write(value: AdtValue, out: StringBuilder, depth: Int) {
        require(depth <= MAX_DEPTH) { "JSON nesting exceeds $MAX_DEPTH" }
        when (value.variant) {
            "Null" -> out.append("null")
            "Bool" -> out.append(value.ixField(0) as Boolean)
            "Number" -> {
                val literal = value.ixField(0) as String
                require(number.matches(literal)) { "Invalid JSON number" }
                out.append(literal)
            }
            "String" -> quote(value.ixField(0) as String, out)
            "Array" -> {
                out.append('[')
                (value.ixField(0) as IxListWrapper).forEachIndexed { index, item ->
                    if (index > 0) out.append(',')
                    write(item as AdtValue, out, depth + 1)
                }
                out.append(']')
            }
            "Object" -> {
                out.append('{')
                val keys = hashSetOf<String>()
                (value.ixField(0) as IxListWrapper).forEachIndexed { index, item ->
                    val pair = item as TupleValue
                    val key = pair.ixField(0) as String
                    require(keys.add(key)) { "Duplicate JSON object key" }
                    if (index > 0) out.append(',')
                    quote(key, out)
                    out.append(':')
                    write(pair.ixField(1) as AdtValue, out, depth + 1)
                }
                out.append('}')
            }
            else -> throw IllegalArgumentException("Invalid JSON variant")
        }
    }

    private fun quote(value: String, out: StringBuilder) {
        validateUnicode(value)
        out.append('"')
        for (c in value) when (c) {
            '"' -> out.append("\\\"")
            '\\' -> out.append("\\\\")
            '\b' -> out.append("\\b")
            '\u000c' -> out.append("\\f")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            else -> if (c < ' ') out.append("\\u" + c.code.toString(16).padStart(4, '0')) else out.append(c)
        }
        out.append('"')
    }

    private fun validateUnicode(value: String) {
        var i = 0
        while (i < value.length) {
            val c = value[i++]
            if (c.isHighSurrogate()) {
                require(i < value.length && value[i++].isLowSurrogate()) { "Unpaired JSON surrogate" }
            } else require(!c.isLowSurrogate()) { "Unpaired JSON surrogate" }
        }
    }

    private class Parser(private val text: String) {
        private var offset = 0
        fun parse(): AdtValue {
            val value = value(0)
            whitespace()
            require(offset == text.length) { "Unexpected trailing JSON at offset $offset" }
            return value
        }

        private fun whitespace() {
            while (offset < text.length && text[offset] in " \t\r\n") offset++
        }

        private fun take(c: Char): Boolean {
            whitespace()
            if (offset == text.length || text[offset] != c) return false
            offset++
            return true
        }

        private fun expect(c: Char) { require(take(c)) { "Expected '$c' at offset $offset" } }

        private fun value(depth: Int): AdtValue {
            require(depth <= MAX_DEPTH) { "JSON nesting exceeds $MAX_DEPTH" }
            whitespace()
            require(offset < text.length) { "Expected JSON value at offset $offset" }
            return when (text[offset]) {
                'n' -> keyword("null", node("Null"))
                't' -> keyword("true", node("Bool", true))
                'f' -> keyword("false", node("Bool", false))
                '"' -> node("String", string())
                '[' -> {
                    offset++
                    val values = arrayListOf<AdtValue>()
                    if (!take(']')) {
                        do { values.add(value(depth + 1)) } while (take(','))
                        expect(']')
                    }
                    node("Array", IxListWrapper(values, TAG))
                }
                '{' -> {
                    offset++
                    val values = arrayListOf<TupleValue>()
                    val keys = hashSetOf<String>()
                    if (!take('}')) {
                        do {
                            whitespace()
                            val key = string()
                            require(keys.add(key)) { "Duplicate JSON object key at offset $offset" }
                            expect(':')
                            values.add(TupleValue(memberTag, arrayOf(key, value(depth + 1))))
                        } while (take(','))
                        expect('}')
                    }
                    node("Object", IxListWrapper(values, memberTag))
                }
                else -> {
                    val start = offset
                    while (offset < text.length && text[offset] !in " \t\r\n,]}") offset++
                    val literal = text.substring(start, offset)
                    require(number.matches(literal)) { "Invalid JSON value at offset $start" }
                    node("Number", literal)
                }
            }
        }

        private fun keyword(word: String, value: AdtValue): AdtValue {
            require(text.startsWith(word, offset)) { "Invalid JSON value at offset $offset" }
            offset += word.length
            return value
        }

        private fun string(): String {
            expect('"')
            val value = StringBuilder()
            while (offset < text.length) {
                val c = text[offset++]
                if (c == '"') return value.toString().also(::validateUnicode)
                require(c >= ' ') { "Unescaped JSON control character at offset $offset" }
                if (c != '\\') { value.append(c); continue }
                require(offset < text.length) { "Incomplete JSON escape" }
                when (val escape = text[offset++]) {
                    '"', '\\', '/' -> value.append(escape)
                    'b' -> value.append('\b')
                    'f' -> value.append('\u000c')
                    'n' -> value.append('\n')
                    'r' -> value.append('\r')
                    't' -> value.append('\t')
                    'u' -> {
                        require(offset + 4 <= text.length) { "Incomplete JSON unicode escape" }
                        val hex = text.substring(offset, offset + 4)
                        require(hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) { "Invalid JSON unicode escape" }
                        value.append(hex.toInt(16).toChar())
                        offset += 4
                    }
                    else -> throw IllegalArgumentException("Invalid JSON escape at offset $offset")
                }
            }
            throw IllegalArgumentException("Unterminated JSON string")
        }
    }
}
