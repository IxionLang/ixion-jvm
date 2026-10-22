import com.kingmang.ixion.modules.JsonModule
import com.kingmang.ixion.runtime.AdtValue
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import com.kingmang.ixion.runtime.TupleValue
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JsonModuleTest {
    @Test fun roundtrips_losslessly_with_unicode_and_all_value_kinds() {
        for (text in listOf("null", "true", "false", "-0", "-1.23e+456", "9".repeat(100),
            "\"Привет 🌍\\n\\t\\b\\f\\r\\\"\\\\\"", "[]", "{}", "[null,true,{\"a\":[1,2]}]")) {
            assertEquals(text, roundtrip(text))
        }
        assertEquals("{\"a\":[1,true,null]}", roundtrip(" \r\n { \"a\" : [1, true, null] } \t"))
        assertEquals("\"🌍\"", roundtrip("\"\\uD83C\\uDF0D\""))
        assertEquals("\"\\u0000\"", roundtrip("\"\\u0000\""))
    }

    @Test fun rejects_invalid_json_and_duplicate_keys() {
        for (text in listOf("", " ", "+1", "01", "-01", ".1", "1.", "1e", "1e+", "NaN", "Infinity",
            "true false", "[1,]", "{\"a\":1,}", "{a:1}", "{\"a\" 1}", "{\"a\":1,\"\\u0061\":2}",
            "\"\\x\"", "\"\\uZZZZ\"", "\"\\u123\"", "\"\\uD800\"", "\"\\uDC00\"", "\"unterminated", "\"raw\nline\"")) {
            assertEquals("Err", JsonModule.parse(text).variant, text)
        }
    }

    @Test fun rejects_invalid_constructed_values_and_excessive_nesting() {
        val number = AdtValue("<json>::Value", "Number", arrayOf("NaN"))
        assertEquals("Err", JsonModule.stringify(number).variant)
        assertEquals("Err", JsonModule.stringify(AdtValue("<json>::Value", "String", arrayOf("\uD800"))).variant)
        // Use a valid number so the duplicate-key check itself must reject this object.
        val valid = AdtValue("<json>::Value", "Number", arrayOf("1"))
        val pair = TupleValue("(string,<json>::Value)", arrayOf("a", valid))
        assertEquals("Err", JsonModule.stringify(AdtValue("<json>::Value", "Object",
            arrayOf(IxListWrapper(listOf(pair, pair), "(string,<json>::Value)")))).variant)
        assertEquals("Err", JsonModule.parse("[".repeat(130) + "0" + "]".repeat(130)).variant)
        assertEquals("Ok", JsonModule.parse("[".repeat(128) + "0" + "]".repeat(128)).variant)
        var tree = AdtValue("<json>::Value", "Null", emptyArray())
        repeat(130) { tree = AdtValue("<json>::Value", "Array", arrayOf(IxListWrapper(listOf(tree), "<json>::Value"))) }
        assertEquals("Err", JsonModule.stringify(tree).variant)
    }

    @Test fun object_lookup_distinguishes_null_from_missing_and_nonobjects() {
        val value = JsonModule.parse("{\"value\":null}").ixField(0) as AdtValue
        val found = JsonModule.get(value, "value")
        assertEquals("Some", found.variant)
        assertEquals("Null", (found.ixField(0) as AdtValue).variant)
        assertEquals("None", JsonModule.get(value, "missing").variant)
        assertEquals("None", JsonModule.get(JsonModule.parse("[]").ixField(0) as AdtValue, "value").variant)
    }

    private fun roundtrip(text: String): String {
        val parsed = JsonModule.parse(text)
        assertEquals("Ok", parsed.variant, parsed.toString())
        val encoded = JsonModule.stringify(parsed.ixField(0) as AdtValue)
        assertEquals("Ok", encoded.variant, encoded.toString())
        return encoded.ixField(0) as String
    }
}
