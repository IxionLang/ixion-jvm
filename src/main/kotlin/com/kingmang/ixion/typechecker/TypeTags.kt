package com.kingmang.ixion.typechecker

import com.kingmang.ixion.runtime.*

/** A tag is a type expression; generic leaves are reified arguments of the enclosing function. */
sealed interface TagExpression {
    data class Text(val value: String) : TagExpression
    data class Parameter(val name: String) : TagExpression
    data class Join(val parts: List<TagExpression>) : TagExpression
    data class Union(val alternatives: List<TagExpression>) : TagExpression
}
object TypeTags {
    fun expression(type: IxType): TagExpression {
        fun combine(prefix: String, args: List<IxType>, suffix: String) = TagExpression.Join(
            listOf(TagExpression.Text(prefix)) + args.flatMapIndexed { i, arg ->
                (if (i == 0) emptyList() else listOf(TagExpression.Text(","))) + expression(arg)
            } + TagExpression.Text(suffix))
        return when (type) {
            is GenericType -> TagExpression.Parameter(type.key)
            is AdtType -> combine("${type.declaration.id.module}::${type.declaration.id.name}" + if (type.arguments.isEmpty()) "" else "[", type.arguments, if (type.arguments.isEmpty()) "" else "]")
            is AppliedStructType -> combine("${type.declarationId.module}::${type.declarationId.name}[", type.arguments, "]")
            is StructType -> TagExpression.Text("${type.declarationId.module}::${type.declarationId.name}")
            is TupleType -> combine("(", type.elements, ")")
            is ListType -> TagExpression.Join(listOf(expression(type.contentType), TagExpression.Text("[]")))
            is LambdaType -> TagExpression.Join(listOf(combine("(", type.parameters.map { it.second }, ")=>"), expression(type.returnType)))
            is UnionType -> TagExpression.Union(type.types.map(::expression))
            is ExternalType -> TagExpression.Text(if (type.foundClass == Any::class.java) "any" else type.name)
            is BuiltInType -> TagExpression.Text(when (type) { BuiltInType.BOOLEAN -> "bool"; BuiltInType.VOID -> "unit"; else -> type.name.lowercase() })
            UnitType -> TagExpression.Text("unit")
            NeverType -> TagExpression.Text("never")
            else -> TagExpression.Text(type.name ?: "unknown")
        }
    }
    fun constant(type: IxType): String {
        fun render(tag: TagExpression): String = when (tag) {
            is TagExpression.Text -> tag.value
            is TagExpression.Parameter -> "${tag.name}@parameter"
            is TagExpression.Join -> tag.parts.joinToString("") { render(it) }
            is TagExpression.Union -> RuntimeTypeTags.union(tag.alternatives.map { render(it) }.toTypedArray())
        }
        return render(expression(type))
    }
}
