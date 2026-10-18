package com.kingmang.ixion.typechecker

import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.types.AppliedType
import com.kingmang.ixion.types.FunctionType
import com.kingmang.ixion.types.PrimitiveType
import com.kingmang.ixion.types.Type
import com.kingmang.ixion.types.TypeParameter
import com.kingmang.ixion.types.UnresolvedType
import com.kingmang.ixion.types.ListType as SemanticList
import com.kingmang.ixion.types.UnionType as SemanticUnion
import com.kingmang.ixion.types.ExternalType as SemanticExternal

/** Boundary between the existing JVM-oriented type objects and immutable semantic keys. */
object SemanticTypes {
    fun of(type: IxType): Type = when (type) {
        is BuiltInType -> when (type) {
            BuiltInType.BOOLEAN -> PrimitiveType.BOOL
            BuiltInType.CHAR -> PrimitiveType.CHAR
            BuiltInType.INT -> PrimitiveType.INT
            BuiltInType.FLOAT -> PrimitiveType.FLOAT
            BuiltInType.DOUBLE -> PrimitiveType.DOUBLE
            BuiltInType.STRING -> PrimitiveType.STRING
            BuiltInType.VOID -> PrimitiveType.UNIT
            BuiltInType.ANY -> PrimitiveType.ANY
        }
        is DefType -> FunctionType(type.parameters.map { of(it.second) }, of(type.returnType))
        is AppliedStructType -> AppliedType(type.declarationId, type.arguments.map(::of))
        is StructType -> AppliedType(type.declarationId)
        is AdtType -> AppliedType(type.declaration.id, type.arguments.map(::of))
        is com.kingmang.ixion.runtime.TupleType -> com.kingmang.ixion.types.TupleType(type.elements.map(::of))
        UnitType -> PrimitiveType.UNIT
        NeverType -> PrimitiveType.NEVER
        is AliasType -> of(type.body)
        is MonomorphizedStruct -> AppliedType(type.struct.declarationId, type.struct.generics.map {
            type.resolved[it]?.let(::of) ?: UnresolvedType(it)
        })
        is ListType -> SemanticList(of(type.contentType))
        is UnionType -> SemanticUnion.of(type.types.map(::of))
        is LambdaType -> FunctionType(type.parameters.map { of(it.second) }, of(type.returnType))
        is GenericType -> TypeParameter(type.owner, type.index, type.key)
        is ExternalType -> when (type.foundClass) {
            Any::class.java -> PrimitiveType.ANY
            else -> SemanticExternal(type.foundClass?.name ?: "<unresolved external>")
        }
        is UnknownType -> UnresolvedType(type.typeName)
        else -> error("Missing semantic type adapter for ${type.javaClass.name}")
    }
}
