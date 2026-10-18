package com.kingmang.ixion.codegen

import com.kingmang.ixion.types.*
import org.objectweb.asm.Opcodes

/** JVM representation is a backend decision, independent of semantic type identity. */
object JvmTypeLayout {
    data class Layout(val descriptor: String, val loadOpcode: Int, val returnOpcode: Int)

    fun value(type: Type): Layout = when (type) {
        PrimitiveType.BOOL -> Layout("Z", Opcodes.ILOAD, Opcodes.IRETURN)
        PrimitiveType.CHAR -> Layout("C", Opcodes.ILOAD, Opcodes.IRETURN)
        PrimitiveType.INT -> Layout("I", Opcodes.ILOAD, Opcodes.IRETURN)
        PrimitiveType.FLOAT -> Layout("F", Opcodes.FLOAD, Opcodes.FRETURN)
        PrimitiveType.DOUBLE -> Layout("D", Opcodes.DLOAD, Opcodes.DRETURN)
        PrimitiveType.STRING -> reference("java/lang/String")
        PrimitiveType.ANY, is UnionType -> reference("java/lang/Object")
        PrimitiveType.UNIT -> reference("com/kingmang/ixion/runtime/UnitValue")
        PrimitiveType.NEVER -> error("never has no JVM value representation")
        is AppliedType -> if (type.declaration.kind == DeclarationKind.ADT) reference("com/kingmang/ixion/runtime/AdtValue")
            else reference(type.declaration.module.replace('\\', '/') + "$" + type.declaration.name)
        is ListType -> reference("com/kingmang/ixion/runtime/CollectionUtil\$IxListWrapper")
        is FunctionType -> {
            reference("com/kingmang/ixion/runtime/ixfunction/IxFunction" + if (type.parameters.size > 4) "N" else type.parameters.size)
        }
        is ExternalType -> reference(type.qualifiedName.replace('.', '/'))
        is TypeParameter -> reference("java/lang/Object")
        is UnresolvedType -> error("Cannot generate a JVM layout for unresolved type ${type.name}")
        is TupleType -> reference("com/kingmang/ixion/runtime/TupleValue")
    }

    fun result(type: Type): Layout = when (type) {
        PrimitiveType.UNIT, PrimitiveType.NEVER -> Layout("V", 0, Opcodes.RETURN)
        else -> value(type)
    }

    fun storage(type: Type): Layout = if (type == PrimitiveType.NEVER) reference("java/lang/Object") else value(type)

    fun methodDescriptor(parameters: List<Type>, result: Type): String =
        parameters.joinToString("", "(", ")") { storage(it).descriptor } + result(result).descriptor

    private fun reference(internalName: String) = Layout("L$internalName;", Opcodes.ALOAD, Opcodes.ARETURN)
}
