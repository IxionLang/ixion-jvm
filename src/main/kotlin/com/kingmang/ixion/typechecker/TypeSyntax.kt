package com.kingmang.ixion.typechecker

import com.kingmang.ixion.ast.*
import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.runtime.ixfunction.*

object TypeSyntax {
    fun resolve(node: TypeStatement, lookup: (String, TypeStatement) -> IxType, error: (String) -> Unit): IxType {
        if (node is UnionTypeStatement) return UnionType.of(node.types.map { resolve(it, lookup, error) })
        fun child(type: TypeStatement) = resolve(type, lookup, error)
        val type = when (node.form) {
            TypeForm.LIST -> ListType(child(node.components.single()))
            TypeForm.TUPLE -> if (node.components.isEmpty()) UnitType else TupleType(node.components.map(::child))
            TypeForm.FUNCTION -> {
                val args = node.components.mapIndexed { i, t -> "p$i" to child(t) }.toMutableList()
                LambdaType(args, child(node.result!!), interfaceFor(args.size))
            }

            TypeForm.NAME -> {
                val name = buildString {
                    append(node.identifier!!.source)
                    var next = node.next.orElse(null)
                    while (next != null) {
                        append("::").append(next.identifier!!.source); next = next.next.orElse(null)
                    }
                }
                val base = when (name) {
                    "unit", "void" -> UnitType; "never" -> NeverType; else -> TypeUtils.getFromString(name) ?: lookup(
                        name,
                        node
                    )
                }
                val args = node.arguments.map(::child)
                val count = when (base) {
                    is AdtType -> base.declaration.parameters.size; is StructType -> base.generics.size; is AliasType -> base.parameters.size; else -> 0
                }
                if (count != args.size) {
                    error("Type $name expects $count arguments, got ${args.size}")
                    UnknownType(name)
                } else when (base) {
                    is AdtType -> base.declaration.applied(args)
                    is StructType -> if (args.isEmpty()) base else AppliedStructType(base, args)
                    is AliasType -> TypeSubstitution.apply(base.body, base.parameters.zip(args).toMap())
                    else -> base
                }
            }
        }
        return if (node.listType) ListType(type) else type
    }

    fun interfaceFor(arity: Int): Class<*> = when (arity) {
        0 -> IxFunction0::class.java; 1 -> IxFunction1::class.java; 2 -> IxFunction2::class.java
        3 -> IxFunction3::class.java; 4 -> IxFunction4::class.java; else -> IxFunctionN::class.java
    }
}
