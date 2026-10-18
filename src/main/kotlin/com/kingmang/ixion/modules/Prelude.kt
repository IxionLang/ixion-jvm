package com.kingmang.ixion.modules

import com.kingmang.ixion.exception.Panic
import com.kingmang.ixion.runtime.CollectionUtil.IxListWrapper
import com.kingmang.ixion.runtime.AdtValue
import com.kingmang.ixion.runtime.ixfunction.IxFunction2
import com.kingmang.ixion.runtime.ixfunction.IxFunction1
import java.util.*
import java.util.stream.IntStream

//std
@Suppress("unused")
object Prelude {

    @JvmStatic
    fun panic(msg: Any?) {
        Panic(msg as String?).send()
    }

    @JvmStatic
    fun println(arg: Any) {
        kotlin.io.println(arg.toString())
    }

    @JvmStatic
    fun print(arg: Any) {
        kotlin.io.print(arg.toString())
    }

    @JvmStatic
    fun readLine(): String? {
        val scanner = Scanner(System.`in`)
        if (scanner.hasNextLine()) {
            val s = scanner.nextLine()
            return s
        }
        scanner.close()
        return null
    }

    @JvmStatic
    fun <T> range(start: Int, stop: Int): MutableIterator<Int?> {
        return IntStream.range(start, stop).boxed().iterator()
    }

    // Retained only to provide migration diagnostics; these methods cannot mutate data.
    @JvmStatic
    fun push(values: IxListWrapper, value: Any?): Nothing =
        error("list.immutable: use std::append")

    @JvmStatic
    fun pop(values: IxListWrapper): Nothing =
        error("list.immutable: use pattern matching")

    @JvmStatic
    fun append(values: IxListWrapper, value: Any?): IxListWrapper =
        IxListWrapper(values.toList() + value, values.name)

    @JvmStatic
    fun concat(left: IxListWrapper, right: IxListWrapper): IxListWrapper =
        IxListWrapper(left.toList() + right.toList(), left.name)

    private fun option(type: String, present: Boolean, value: Any? = null): AdtValue =
        AdtValue("<prelude>::Option[$type]", if (present) "Some" else "None",
            if (present) arrayOf(value) else emptyArray())

    @JvmStatic
    fun get(values: IxListWrapper, index: Int): AdtValue =
        if (index >= 0 && index < values.size()) option(values.name, true, values.element(index))
        else option(values.name, false)

    @JvmStatic
    fun updated(values: IxListWrapper, index: Int, value: Any?): AdtValue {
        if (index < 0 || index >= values.size()) return option(values.name + "[]", false)
        val result = values.toMutableList()
        result[index] = value
        return option(values.name + "[]", true, IxListWrapper(result, values.name))
    }

    @JvmStatic
    fun map(values: IxListWrapper, transform: IxFunction1<Any?, Any?>, types: Array<String>): IxListWrapper {
        val result = ArrayList<Any?>(values.size())
        for (value in values) result.add(transform.apply(value))
        return IxListWrapper(result, types[1])
    }

    @JvmStatic
    fun filter(values: IxListWrapper, predicate: IxFunction1<Any?, Boolean>): IxListWrapper {
        val result = ArrayList<Any?>()
        for (value in values) if (predicate.apply(value) == true) result.add(value)
        return IxListWrapper(result, values.name)
    }

    @JvmStatic
    fun fold(values: IxListWrapper, initial: Any?, transform: IxFunction2<Any?, Any?, Any?>): Any? {
        var accumulator = initial
        for (value in values) accumulator = transform.apply(accumulator, value)
        return accumulator
    }

    @JvmStatic
    fun len(r: Any?): Int {
        return when (r) {
            is IxListWrapper -> r.size()
            is String -> r.length
            else -> error("len expects an immutable list or a string")
        }
    }
}
