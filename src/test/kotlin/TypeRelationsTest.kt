import com.kingmang.ixion.codegen.JvmTypeLayout
import com.kingmang.ixion.runtime.BuiltInType
import com.kingmang.ixion.runtime.StructType
import com.kingmang.ixion.typechecker.SemanticTypes
import com.kingmang.ixion.typechecker.TypeResolver
import com.kingmang.ixion.types.*
import com.kingmang.ixion.types.PrimitiveType.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TypeRelationsTest {
    private val box = DeclarationId("domain/model", "Box", DeclarationKind.STRUCT)

    @Test
    fun any_is_only_an_expected_top_type() {
        assertTrue(TypeRelations.isAssignable(ANY, INT))
        assertTrue(TypeRelations.isAssignable(ANY, UNIT))
        assertFalse(TypeRelations.isAssignable(INT, ANY))
        assertTrue(TypeResolver.typesMatch(BuiltInType.ANY, BuiltInType.INT))
        assertFalse(TypeResolver.typesMatch(BuiltInType.INT, BuiltInType.ANY))
    }

    @Test
    fun numeric_widening_has_a_direction_and_keeps_bool_separate() {
        assertTrue(TypeRelations.isAssignable(INT, CHAR))
        assertTrue(TypeRelations.isAssignable(DOUBLE, INT))
        assertFalse(TypeRelations.isAssignable(CHAR, INT))
        assertFalse(TypeRelations.isAssignable(INT, DOUBLE))
        assertFalse(TypeRelations.isAssignable(INT, BOOL))
    }

    @Test
    fun lists_and_generic_products_are_invariant() {
        assertFalse(TypeRelations.isAssignable(ListType(ANY), ListType(INT)))
        assertFalse(TypeRelations.isAssignable(ListType(FLOAT), ListType(INT)))
        assertFalse(TypeRelations.isAssignable(AppliedType(box, listOf(ANY)), AppliedType(box, listOf(INT))))
        assertTrue(TypeRelations.isAssignable(AppliedType(box, listOf(INT)), AppliedType(box, listOf(INT))))
        assertFalse(TypeResolver.typesMatch(com.kingmang.ixion.runtime.ListType(BuiltInType.ANY),
            com.kingmang.ixion.runtime.ListType(BuiltInType.INT)))
    }

    @Test
    fun function_parameters_and_result_are_invariant_and_names_are_irrelevant() {
        val left = FunctionType(listOf(INT), INT)
        assertFalse(TypeRelations.isAssignable(left, FunctionType(listOf(INT), FLOAT)))
        assertFalse(TypeRelations.isAssignable(FunctionType(listOf(FLOAT), INT), left))
        val a = com.kingmang.ixion.runtime.LambdaType(arrayListOf("x" to BuiltInType.INT), BuiltInType.INT,
            com.kingmang.ixion.runtime.ixfunction.IxFunction1::class.java)
        val b = a.copy(parameters = arrayListOf("y" to BuiltInType.INT))
        assertTrue(TypeResolver.typesMatch(a, b))
    }

    @Test
    fun union_normalization_flattens_deduplicates_and_removes_bottom() {
        val left = UnionType.of(listOf(INT, STRING))
        val right = UnionType.of(listOf(STRING, UnionType.of(listOf(NEVER, INT)), INT))
        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertEquals(INT, UnionType.of(listOf(INT, NEVER, INT)))
        assertEquals(ANY, UnionType.of(listOf(left, ANY)))
        assertEquals(NEVER, UnionType.of(emptyList()))
    }

    @Test
    fun union_inclusion_prefers_exact_and_rejects_ambiguous_widening() {
        val numbers = UnionType.of(listOf(INT, FLOAT)) as UnionType
        assertEquals(INT, TypeRelations.injectionTarget(numbers, INT))
        assertNull(TypeRelations.injectionTarget(numbers, CHAR))
        assertFalse(TypeRelations.isAssignable(numbers, CHAR))
        assertTrue(TypeRelations.isAssignable(numbers, INT))
        assertEquals(FLOAT, TypeRelations.injectionTarget(UnionType.of(listOf(FLOAT, STRING)) as UnionType, INT))
        assertTrue(TypeRelations.isAssignable(UnionType.of(listOf(INT, FLOAT, STRING)), numbers))
        assertFalse(TypeRelations.isAssignable(numbers, UnionType.of(listOf(INT, STRING))))
    }

    @Test
    fun common_type_uses_numeric_join_or_union_without_any_fallback() {
        assertEquals(FLOAT, TypeRelations.commonType(listOf(INT, NEVER, FLOAT)))
        assertEquals(UnionType.of(listOf(BOOL, STRING)), TypeRelations.commonType(listOf(BOOL, STRING)))
        assertEquals(NEVER, TypeRelations.commonType(emptyList()))
        assertTrue(TypeRelations.isAssignable(AppliedType(box), NEVER))
        assertFalse(TypeRelations.isAssignable(INT, UnresolvedType("Missing")))
        assertFalse(TypeRelations.isAssignable(ANY, ListType(UnresolvedType("Missing"))))
    }

    @Test
    fun substitution_is_recursive_scoped_and_normalizes_unions() {
        val t = TypeParameter(box, 0, "T")
        val other = TypeParameter(box.copy(module = "other"), 0, "T")
        assertEquals(t, TypeParameter(box, 0, "Renamed"))
        val source = FunctionType(listOf(ListType(t), AppliedType(box, listOf(other))),
            TupleType(listOf(t, UnionType.of(listOf(t, INT)))))
        val result = TypeRelations.substitute(source, mapOf(t to INT))
        assertEquals(FunctionType(listOf(ListType(INT), AppliedType(box, listOf(other))),
            TupleType(listOf(INT, INT))), result)
    }

    @Test
    fun semantic_keys_snapshot_collections() {
        val arguments = arrayListOf<Type>(INT)
        val applied = AppliedType(box, arguments)
        val tuple = TupleType(arguments)
        val function = FunctionType(arguments, STRING)
        val keySet = hashSetOf<Type>(applied, tuple, function)
        arguments[0] = STRING
        assertTrue(AppliedType(box, listOf(INT)) in keySet)
        assertTrue(TupleType(listOf(INT)) in keySet)
        assertTrue(FunctionType(listOf(INT), STRING) in keySet)
        assertThrows(UnsupportedOperationException::class.java) { (applied.arguments as MutableList).clear() }
    }

    @Test
    fun nominal_identity_is_module_qualified_and_stable_during_resolution() {
        val first = StructType("Box", arrayListOf(), arrayListOf(), box)
        val second = StructType("Box", arrayListOf("different" to BuiltInType.INT), arrayListOf(), box)
        val foreign = StructType("Box", arrayListOf(), arrayListOf(), box.copy(module = "other"))
        val map = hashMapOf(first to "registered")
        first.parameters.add("recursive" to first)
        first.qualifiedName = "generated/Name"
        first.name = "generatedBox"
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertEquals("registered", map[second])
        assertNotEquals(first, foreign)
        assertFalse(TypeResolver.typesMatch(first, foreign))
        assertEquals(AppliedType(box), SemanticTypes.of(first))
    }

    @Test
    fun specialized_struct_identity_includes_arguments_in_declaration_order() {
        val type = StructType("Box", arrayListOf(), arrayListOf("T"), box)
        val ints = com.kingmang.ixion.runtime.MonomorphizedStruct(type).apply { resolved["T"] = BuiltInType.INT }
        val strings = com.kingmang.ixion.runtime.MonomorphizedStruct(type).apply { resolved["T"] = BuiltInType.STRING }
        assertFalse(TypeResolver.typesMatch(ints, strings))
        assertEquals(AppliedType(box, listOf(INT)), SemanticTypes.of(ints))
    }

    @Test
    fun layout_is_independent_of_semantic_identity() {
        assertEquals("(ID)V", JvmTypeLayout.methodDescriptor(listOf(INT, DOUBLE), UNIT))
        assertEquals("Lcom/kingmang/ixion/runtime/UnitValue;", JvmTypeLayout.value(UNIT).descriptor)
        assertEquals(JvmTypeLayout.value(ANY), JvmTypeLayout.value(UnionType.of(listOf(INT, STRING))))
        assertNotEquals(ANY, UnionType.of(listOf(INT, STRING)))
        assertThrows(IllegalStateException::class.java) { JvmTypeLayout.value(NEVER) }
        assertThrows(IllegalStateException::class.java) { JvmTypeLayout.value(UnresolvedType("Missing")) }
    }
}
