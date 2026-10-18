package com.kingmang.ixion.runtime

import java.util.Collections
import java.util.WeakHashMap

/** Reified function signatures without keeping otherwise unreachable closures alive. */
object FunctionTags {
    private val signatures = Collections.synchronizedMap(WeakHashMap<Any, String>())
    @JvmStatic fun register(function: Any, signature: String): Any {
        signatures[function] = signature
        return function
    }
    fun signature(function: Any): String? = signatures[function]
}
