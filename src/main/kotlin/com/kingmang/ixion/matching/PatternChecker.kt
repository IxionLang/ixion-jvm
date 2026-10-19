package com.kingmang.ixion.matching

import com.kingmang.ixion.ast.*
import com.kingmang.ixion.runtime.*
import com.kingmang.ixion.typechecker.*

class PatternChecker(
    private val resolve: (TypeStatement) -> IxType,
    private val lookup: (String) -> IxType?,
    private val fail: (Pattern, String) -> Nothing
) {
    private val tags = arrayListOf<IxType>()
    private val bindings = arrayListOf<PatternBinding>()
    private val names = hashSetOf<String>()
    fun check(pattern: Pattern, expected: IxType): PatternPlan {
        val (test, cover) = compile(pattern, expected, "")
        return PatternPlan(test, tags.toList(), bindings.toList(), cover)
    }

    private fun bind(pattern: Pattern, name: String, type: IxType, path: String) {
        if (!names.add(name)) fail(pattern, "pattern.duplicate_binding: $name")
        bindings.add(PatternBinding(name, type, path))
    }

    private fun tag(type: IxType): Int {
        tags.add(type); return tags.lastIndex
    }

    private fun path(base: String, part: String) = if (base.isEmpty()) part else "$base.$part"
    private fun named(node: TypeStatement): String = node.identifier?.source ?: "<structural type>"
    private fun owner(pattern: Pattern, node: TypeStatement, expected: IxType): IxType {
        if (node.form != TypeForm.NAME) fail(pattern, "pattern.expected_nominal_type")
        val raw = lookup(named(node)) ?: fail(pattern, "pattern.unknown_type: ${named(node)}")
        if (node.arguments.isNotEmpty()) return resolve(node)
        return when {
            raw is AdtType && expected is AdtType && raw.declaration.id == expected.declaration.id -> expected
            raw is StructType && expected is AppliedStructType && raw.declarationId == expected.declarationId -> expected
            raw is AdtType && raw.declaration.parameters.isNotEmpty() -> fail(
                pattern,
                "pattern.type_arguments_required: ${raw.name}"
            )

            else -> raw
        }
    }

    private fun generic(type: IxType): Boolean = when (type) {
        is GenericType -> true
        is AdtType -> type.arguments.any(::generic)
        is AppliedStructType -> type.arguments.any(::generic)
        is ListType -> generic(type.contentType)
        is TupleType -> type.elements.any(::generic)
        is LambdaType -> type.parameters.any { generic(it.second) } || generic(type.returnType)
        else -> false
    }

    private fun overlap(a: IxType, b: IxType): Boolean = when {
        a is GenericType || b is GenericType -> true
        a is AdtType && b is AdtType -> a.declaration.id == b.declaration.id && (generic(a) || generic(b))
        a is StructType && b is StructType -> a.declarationId == b.declarationId && (generic(a) || generic(b))
        a is ListType && b is ListType -> generic(a) || generic(b)
        a is TupleType && b is TupleType -> a.elements.size == b.elements.size && (generic(a) || generic(b))
        a is LambdaType && b is LambdaType -> a.parameters.size == b.parameters.size && (generic(a) || generic(b))
        else -> false
    }

    private fun compile(pattern: Pattern, expected: IxType, at: String): Pair<PatternTest, CoverPattern> {
        if (expected is UnionType && pattern !is WildcardPattern && pattern !is BindingPattern) {
            val alternatives = expected.types.toList()
            if (alternatives.indices.any { i -> alternatives.drop(i + 1).any { overlap(alternatives[i], it) } })
                fail(pattern, "pattern.overlapping_generic_union: use named ADT variants or a covering binding")
        }
        if (expected is UnionType && pattern !is WildcardPattern && pattern !is BindingPattern && pattern !is TypePattern) {
            val candidates = expected.types.filter { type ->
                when (pattern) {
                    is ConstructorPattern -> type is AdtType && (lookup(named(pattern.owner)) as? AdtType)?.declaration?.id == type.declaration.id &&
                            (pattern.owner.arguments.isEmpty() || SemanticTypes.of(resolve(pattern.owner)) == SemanticTypes.of(
                                type
                            ))

                    is StructPattern -> type is StructType && (lookup(named(pattern.type)) as? StructType)?.declarationId == type.declarationId &&
                            (pattern.type.arguments.isEmpty() || SemanticTypes.of(resolve(pattern.type)) == SemanticTypes.of(
                                type
                            ))

                    is TuplePattern -> type is TupleType && type.elements.size == pattern.elements.size
                    is ListPattern -> type is ListType
                    is LiteralPattern -> SemanticTypes.of(type) == SemanticTypes.of(literalType(pattern))
                    else -> false
                }
            }
            val type = candidates.singleOrNull() ?: fail(pattern, "pattern.ambiguous_union_alternative")
            val nested = compile(pattern, type, at)
            return nested.first to CoverPattern(unionConstructor(type), listOf(nested.second))
        }
        return when (pattern) {
            is WildcardPattern -> PatternTest.AnyValue to CoverPattern()
            is BindingPattern -> {
                bind(pattern, pattern.name.source, expected, at); PatternTest.AnyValue to CoverPattern()
            }

            is TypePattern -> {
                val target = resolve(pattern.type)
                if (expected !== BuiltInType.ANY && (if (expected is UnionType) expected.types.none {
                        SemanticTypes.of(
                            it
                        ) == SemanticTypes.of(target)
                    }
                    else SemanticTypes.of(expected) != SemanticTypes.of(target))) fail(
                    pattern,
                    "pattern.impossible_type: ${target.name}"
                )
                bind(pattern, pattern.binding.source, target, at)
                (if (expected !is UnionType && expected !== BuiltInType.ANY || target === BuiltInType.ANY) PatternTest.AnyValue else PatternTest.TypeTag(
                    tag(target)
                )) to if (expected is UnionType) CoverPattern(
                    unionConstructor(target),
                    listOf(CoverPattern())
                ) else if (expected === BuiltInType.ANY && target !== BuiltInType.ANY) CoverPattern(
                    unionConstructor(
                        target
                    ), listOf(CoverPattern())
                ) else CoverPattern()
            }

            is LiteralPattern -> {
                val type = literalType(pattern)
                if (expected !== BuiltInType.ANY && SemanticTypes.of(type) != SemanticTypes.of(expected)) fail(
                    pattern,
                    "pattern.literal_type_mismatch"
                )
                val kind = TypeTags.constant(type)
                val source = (pattern.value as? LiteralExpression)?.literal?.source ?: "()"
                val text = when (type) {
                    BuiltInType.INT -> source.toInt().toString(); BuiltInType.FLOAT -> source.toFloat()
                        .let { if (it == 0.0f) "0.0" else it.toString() }; BuiltInType.DOUBLE -> source.toDouble()
                        .let { if (it == 0.0) "0.0" else it.toString() }; else -> source
                }
                val constructor = CoverConstructor("literal:$kind:$text", text, emptyList())
                PatternTest.Literal(kind, text) to CoverPattern(constructor)
            }

            is ConstructorPattern -> {
                val type = owner(pattern, pattern.owner, expected) as? AdtType ?: fail(pattern, "pattern.expected_adt")
                if (expected !== BuiltInType.ANY && SemanticTypes.of(type) != SemanticTypes.of(expected)) fail(
                    pattern,
                    "pattern.wrong_adt: ${type.name}"
                )
                val variant = type.declaration.variants.find { it.name == pattern.variant.source }
                    ?: fail(pattern, "pattern.unknown_variant: ${pattern.variant.source}")
                val fields = type.fields(variant)
                if (fields.size != pattern.fields.size) fail(
                    pattern,
                    "pattern.arity: ${variant.name} expects ${fields.size} fields"
                )
                val children = fields.indices.map { compile(pattern.fields[it], fields[it].second, path(at, "f$it")) }
                PatternTest.Product("adt", tag(type), variant.name, children.map { it.first }) to
                        CoverPattern(adtConstructor(type, variant), children.map { it.second })
            }

            is StructPattern -> {
                val type =
                    owner(pattern, pattern.type, expected) as? StructType ?: fail(pattern, "pattern.expected_struct")
                if (expected !== BuiltInType.ANY && SemanticTypes.of(type) != SemanticTypes.of(expected)) fail(
                    pattern,
                    "pattern.wrong_struct"
                )
                val supplied = pattern.fields.map { it.first.source }
                if (supplied.distinct().size != supplied.size) fail(pattern, "pattern.duplicate_field")
                if (supplied.any { name -> type.parameters.none { it.first == name } }) fail(
                    pattern,
                    "pattern.unknown_field"
                )
                val children = type.parameters.mapIndexed { index, field ->
                    val child = pattern.fields.find { it.first.source == field.first }?.second
                    if (child == null) PatternTest.AnyValue to CoverPattern() else compile(
                        child,
                        field.second,
                        path(at, "f$index")
                    )
                }
                PatternTest.Product("struct", tag(type), "", children.map { it.first }) to
                        CoverPattern(productConstructor(type), children.map { it.second })
            }

            is TuplePattern -> {
                val type = expected as? TupleType ?: fail(pattern, "pattern.expected_tuple")
                if (type.elements.size != pattern.elements.size) fail(pattern, "pattern.tuple_arity")
                val children =
                    type.elements.indices.map { compile(pattern.elements[it], type.elements[it], path(at, "f$it")) }
                PatternTest.Product("tuple", tag(type), "", children.map { it.first }) to
                        CoverPattern(productConstructor(type), children.map { it.second })
            }

            is ListPattern -> {
                val type = expected as? ListType ?: fail(pattern, "pattern.expected_list")
                val children = pattern.elements.mapIndexed { index, child ->
                    compile(
                        child,
                        type.contentType,
                        path(at, "i$index")
                    )
                }
                val rest = pattern.rest?.let { compile(it, type, path(at, "r${children.size}")) }
                var cover = rest?.second ?: CoverPattern(nilConstructor())
                for (child in children.asReversed()) cover =
                    CoverPattern(consConstructor(type), listOf(child.second, cover))
                PatternTest.Sequence(children.map { it.first }, rest?.first) to cover
            }
        }
    }

    private fun literalType(pattern: LiteralPattern): IxType = if (pattern.value is UnitExpression) UnitType
    else TypeUtils.getFromToken((pattern.value as LiteralExpression).literal.type)

    companion object {
        fun unionConstructor(type: IxType) =
            CoverConstructor("type:${TypeTags.constant(type)}", TypeTags.constant(type), listOf(type))

        fun adtConstructor(type: AdtType, variant: AdtVariant) = CoverConstructor(
            "variant:${variant.name}",
            "${type.declaration.id.name}::${variant.name}",
            type.fields(variant).map { it.second })

        fun productConstructor(type: IxType) = CoverConstructor(
            "product:${TypeTags.constant(type)}", type.name ?: "product", when (type) {
                is TupleType -> type.elements; is StructType -> type.parameters.map { it.second }; else -> emptyList()
            }
        )

        fun nilConstructor() = CoverConstructor("nil", "[]", emptyList())
        fun consConstructor(type: ListType) = CoverConstructor("cons", "[_, .._]", listOf(type.contentType, type))
        private fun inhabitable(
            type: IxType,
            active: Set<com.kingmang.ixion.types.Type> = emptySet(),
            depth: Int = 0
        ): Boolean {
            if (type === NeverType) return false
            if (depth > 64) return true // Unknown is conservative: never omit a possibly inhabited constructor.
            val key = SemanticTypes.of(type)
            if (key in active) return false
            val next = active + key
            return when (type) {
                is AdtType -> type.declaration.variants.any { variant ->
                    type.fields(variant).all { inhabitable(it.second, next, depth + 1) }
                }

                is StructType -> type.parameters.all { inhabitable(it.second, next, depth + 1) }
                is TupleType -> type.elements.all { inhabitable(it, next, depth + 1) }
                is UnionType -> type.types.any { inhabitable(it, next, depth + 1) }
                else -> true // Lists have [], functions can diverge, and type parameters are unconstrained.
            }
        }

        fun universe(type: IxType): List<CoverConstructor>? = when (type) {
            is UnionType -> type.types.filter { inhabitable(it) }.map(::unionConstructor)
            is AdtType -> type.declaration.variants.filter { variant ->
                type.fields(variant).all { inhabitable(it.second) }
            }.map { adtConstructor(type, it) }

            is TupleType, is StructType -> if (inhabitable(type)) listOf(productConstructor(type)) else emptyList()
            is ListType -> listOf(nilConstructor(), consConstructor(type))
            BuiltInType.BOOLEAN -> listOf(
                CoverConstructor("literal:bool:true", "true", emptyList()),
                CoverConstructor("literal:bool:false", "false", emptyList())
            )

            UnitType, BuiltInType.VOID -> listOf(CoverConstructor("literal:unit:()", "()", emptyList()))
            NeverType -> emptyList()
            else -> null
        }
    }
}
