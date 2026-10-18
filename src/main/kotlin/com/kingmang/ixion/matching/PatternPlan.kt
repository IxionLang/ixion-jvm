package com.kingmang.ixion.matching

import com.kingmang.ixion.runtime.IxType
import java.io.*
import java.util.Base64

/** Shared, effect-free decision plan consumed by ASM and Java code generation. */
sealed interface PatternTest {
    data object AnyValue : PatternTest
    data class Literal(val kind: String, val text: String) : PatternTest
    data class Product(val kind: String, val tag: Int, val variant: String, val fields: List<PatternTest>) : PatternTest
    data class Sequence(val elements: List<PatternTest>, val rest: PatternTest?) : PatternTest
    data class TypeTag(val index: Int) : PatternTest
}
data class PatternBinding(val name: String, val type: IxType, val path: String)
data class PatternPlan(val test: PatternTest, val tags: List<IxType>, val bindings: List<PatternBinding>, val coverage: CoverPattern) {
    val encoded: String by lazy { PatternCodec.encode(test) }
}
data class CoverConstructor(val key: String, val label: String, val fields: List<IxType>)
data class CoverPattern(val constructor: CoverConstructor? = null, val fields: List<CoverPattern> = emptyList())

object PatternCodec {
    fun encode(test: PatternTest): String {
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { write(it, test) }
        return Base64.getEncoder().encodeToString(buffer.toByteArray())
    }
    fun decode(value: String): PatternTest = DataInputStream(ByteArrayInputStream(Base64.getDecoder().decode(value))).use(::read)
    private fun text(out: DataOutputStream, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size); out.write(bytes)
    }
    private fun text(input: DataInputStream): String {
        val bytes = ByteArray(input.readInt()); input.readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }
    private fun write(out: DataOutputStream, test: PatternTest) {
        when (test) {
            PatternTest.AnyValue -> out.writeByte(0)
            is PatternTest.Literal -> { out.writeByte(1); text(out, test.kind); text(out, test.text) }
            is PatternTest.Product -> {
                out.writeByte(2); text(out, test.kind); out.writeInt(test.tag); text(out, test.variant)
                out.writeInt(test.fields.size); test.fields.forEach { write(out, it) }
            }
            is PatternTest.Sequence -> {
                out.writeByte(3); out.writeInt(test.elements.size); test.elements.forEach { write(out, it) }
                out.writeBoolean(test.rest != null); test.rest?.let { write(out, it) }
            }
            is PatternTest.TypeTag -> { out.writeByte(4); out.writeInt(test.index) }
        }
    }
    private fun read(input: DataInputStream): PatternTest = when (input.readUnsignedByte()) {
        0 -> PatternTest.AnyValue
        1 -> PatternTest.Literal(text(input), text(input))
        2 -> { val kind = text(input); val tag = input.readInt(); val variant = text(input)
            PatternTest.Product(kind, tag, variant, List(input.readInt()) { read(input) }) }
        3 -> { val elements = List(input.readInt()) { read(input) }; PatternTest.Sequence(elements, if (input.readBoolean()) read(input) else null) }
        4 -> PatternTest.TypeTag(input.readInt())
        else -> error("Invalid compiler pattern plan")
    }
}
