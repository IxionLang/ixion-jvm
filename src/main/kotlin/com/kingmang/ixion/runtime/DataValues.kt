package com.kingmang.ixion.runtime

/** Compiler-generated products expose read-only indexed access to the matching engine. */
interface IxDataValue {
    fun ixType(): String
    fun ixField(index: Int): Any?
}
class AdtValue(private val tag: String, val variant: String, fields: Array<Any?>) : IxDataValue {
    private val fields = fields.copyOf()
    override fun ixType(): String = tag
    override fun ixField(index: Int): Any? = fields[index]
    override fun toString(): String = variant + fields.joinToString(", ", "(", ")")
}
class TupleValue(private val tag: String, fields: Array<Any?>) : IxDataValue {
    private val fields = fields.copyOf()
    override fun ixType(): String = tag
    override fun ixField(index: Int): Any? = fields[index]
    override fun toString(): String = fields.joinToString(", ", "(", ")")
}

/** Used only inside generated Java expressions; caught at the enclosing Ixion function. */
class CaseReturn(val value: Any?) : RuntimeException(null, null, false, false)
