import com.kingmang.ixion.modules.Prelude
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import com.kingmang.ixion.runtime.MatchRuntime
import com.kingmang.ixion.runtime.ixfunction.IxFunction1
import com.kingmang.ixion.runtime.ixfunction.IxFunction2
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class ImmutableListRuntimeTest {
    @Test fun construction_copies_input_and_iterator_cannot_remove() {
        val input = arrayListOf<Any?>(1, 2)
        val values = IxListWrapper(input, "int")
        input[0] = 99
        input.clear()
        assertEquals(listOf(1, 2), values.toList())
        val iterator = values.iterator() as MutableIterator<Any?>
        assertEquals(1, iterator.next())
        assertThrows(UnsupportedOperationException::class.java) { iterator.remove() }
        assertEquals(listOf(1, 2), values.toList())
        assertTrue(Modifier.isFinal(IxListWrapper::class.java.modifiers))
        assertTrue(IxListWrapper::class.java.declaredFields.all {
            Modifier.isPrivate(it.modifiers) && Modifier.isFinal(it.modifiers)
        })
        assertFalse(IxListWrapper::class.java.methods.any {
            java.util.Collection::class.java.isAssignableFrom(it.returnType) || it.returnType.isArray
        }, "No mutable storage getter, component or copy API")
    }

    @Test fun all_list_results_and_pattern_tails_preserve_old_values_and_tags() {
        val original = IxListWrapper(arrayListOf(1, 2), "int")
        val appended = Prelude.append(original, 3)
        val joined = Prelude.concat(original, appended)
        val updated = Prelude.updated(original, 0, 42)
        assertEquals("<prelude>::Option[int[]]", updated.ixType())
        assertEquals("Some", updated.variant)
        val changed = updated.ixField(0) as IxListWrapper
        assertEquals(listOf(42, 2), changed.toList())
        assertEquals(listOf(1, 2), original.toList())
        assertEquals(listOf(1, 2, 3), appended.toList())
        assertEquals(listOf(1, 2, 1, 2, 3), joined.toList())
        val tail = MatchRuntime.extract(original, "r1") as IxListWrapper
        assertEquals("int[]", MatchRuntime.typeTag(tail))
        assertEquals(listOf(2, 9), Prelude.append(tail, 9).toList())
        assertEquals(listOf(2), tail.toList())
        for (index in listOf(Int.MIN_VALUE, -1, 2, Int.MAX_VALUE)) {
            assertEquals("None", Prelude.get(original, index).variant)
            assertEquals("None", Prelude.updated(original, index, 0).variant)
        }
        assertEquals("<prelude>::Option[int]", Prelude.get(original, 0).ixType())
        assertEquals(1, Prelude.get(original, 0).ixField(0))
        val empty = IxListWrapper("int")
        assertEquals(0, Prelude.len(empty))
        assertEquals("None", Prelude.get(empty, 0).variant)
        assertEquals("None", Prelude.updated(empty, 0, 1).variant)
    }

    @Test fun callback_failure_stops_iteration_and_never_changes_the_source() {
        val values = IxListWrapper(arrayListOf(1, 2, 3), "int")
        for (operation in listOf("map", "filter", "fold")) {
            val visited = arrayListOf<Any?>()
            val failure = IllegalStateException("callback")
            fun visit(value: Any?) {
                visited.add(value)
                if (value == 2) throw failure
            }
            val thrown = assertThrows(IllegalStateException::class.java) {
                when (operation) {
                    "map" -> Prelude.map(values, IxFunction1 { value -> visit(value); value }, arrayOf("int", "int"))
                    "filter" -> Prelude.filter(values, IxFunction1 { value -> visit(value); true })
                    "fold" -> Prelude.fold(values, 0, IxFunction2 { sum, value -> visit(value); sum })
                }
            }
            assertSame(failure, thrown)
            assertEquals(listOf(1, 2), visited)
            assertEquals(listOf(1, 2, 3), values.toList())
        }
    }

    @Test fun language_lists_are_not_compatible_with_mutable_native_lists() {
        val languageList = com.kingmang.ixion.runtime.ListType(com.kingmang.ixion.runtime.BuiltInType.INT)
        assertFalse(com.kingmang.ixion.typechecker.TypeResolver.typesMatch(
            com.kingmang.ixion.runtime.ExternalType(java.util.List::class.java), languageList))
    }
}
