package com.kingmang.ixion.runtime

import com.kingmang.ixion.types.DeclarationId
import org.objectweb.asm.Opcodes

abstract class ReferenceType : IxType {
    override val defaultValue: Any? get() = null
    override val internalName: String get() = descriptor!!.removePrefix("L").removeSuffix(";")
    override val loadVariableOpcode: Int get() = Opcodes.ALOAD
    override val returnOpcode: Int get() = Opcodes.ARETURN
    override val typeClass: Class<*>? get() = null
    override val isNumeric: Boolean get() = false
    override fun kind(): String = "data"
}

class AdtDeclaration(val id: DeclarationId, val parameters: List<GenericType>) {
    val variants: MutableList<AdtVariant> = arrayListOf()
    fun applied(arguments: List<IxType>) = AdtType(this, arguments.toList())
}
data class AdtVariant(val name: String, val fields: List<Pair<String, IxType>>)
class AdtType(val declaration: AdtDeclaration, val arguments: List<IxType>) : ReferenceType() {
    override val descriptor: String get() = "Lcom/kingmang/ixion/runtime/AdtValue;"
    override val name: String get() = declaration.id.name + if (arguments.isEmpty()) "" else arguments.joinToString(", ", "[", "]")
    fun fields(variant: AdtVariant): List<Pair<String, IxType>> {
        val map = declaration.parameters.zip(arguments).toMap()
        return variant.fields.map { it.first to TypeSubstitution.apply(it.second, map) }
    }
    override fun equals(other: Any?): Boolean = other is AdtType && declaration.id == other.declaration.id && arguments == other.arguments
    override fun hashCode(): Int = 31 * declaration.id.hashCode() + arguments.hashCode()
    override fun toString(): String = name
}
class ConstructorType(val owner: AdtDeclaration, val variant: AdtVariant, val arguments: List<IxType>? = null) : ReferenceType() {
    override val descriptor: String get() = "Ljava/lang/Object;"
    override val name: String get() = "${owner.id.name}::${variant.name}"
}
class AppliedStructType(val base: StructType, val arguments: List<IxType>) : StructType(
    base.name, arrayListOf(), arrayListOf(), base.declarationId
) {
    init { qualifiedName = base.qualifiedName; parentName = base.parentName }
    override val parameters: MutableList<Pair<String, IxType>> get() = base.parameters.map {
        it.first to TypeSubstitution.apply(it.second,
            base.generics.mapIndexed { index, name -> GenericType(name, base.declarationId, index) }.zip(arguments).toMap())
    }.toMutableList()
    override fun equals(other: Any?): Boolean = other is AppliedStructType && declarationId == other.declarationId && arguments == other.arguments
    override fun hashCode(): Int = 31 * declarationId.hashCode() + arguments.hashCode()
}
class TupleType(val elements: List<IxType>) : ReferenceType() {
    override val descriptor: String get() = "Lcom/kingmang/ixion/runtime/TupleValue;"
    override val name: String get() = elements.joinToString(", ", "(", ")")
    override fun equals(other: Any?): Boolean = other is TupleType && elements == other.elements
    override fun hashCode(): Int = elements.hashCode()
    override fun toString(): String = name
}
class AliasType(val parameters: List<GenericType>, var body: IxType) : ReferenceType() {
    override val descriptor: String get() = body.descriptor!!
    override val name: String get() = body.name!!
}
object UnitType : ReferenceType() {
    override val descriptor: String get() = "Lcom/kingmang/ixion/runtime/UnitValue;"
    override val name: String get() = "unit"
}
object NeverType : IxType by BuiltInType.VOID {
    override val name: String get() = "never"
    // Uninhabited parameter/field slots still need a legal JVM descriptor.
    override val descriptor: String get() = "Ljava/lang/Object;"
    override val internalName: String get() = "java/lang/Object"
    override val loadVariableOpcode: Int get() = Opcodes.ALOAD
}

object TypeSubstitution {
    fun apply(type: IxType, arguments: Map<GenericType, IxType>): IxType = when (type) {
        is GenericType -> arguments[type] ?: type
        is AdtType -> type.declaration.applied(type.arguments.map { apply(it, arguments) })
        is AppliedStructType -> AppliedStructType(type.base, type.arguments.map { apply(it, arguments) })
        is ListType -> ListType(apply(type.contentType, arguments))
        is TupleType -> TupleType(type.elements.map { apply(it, arguments) })
        is LambdaType -> LambdaType(type.parameters.map { it.first to apply(it.second, arguments) }.toMutableList(), apply(type.returnType, arguments), type.functionalInterface)
        is UnionType -> UnionType.of(type.types.map { apply(it, arguments) })
        else -> type
    }
}
