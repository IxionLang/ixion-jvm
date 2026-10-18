package com.kingmang.ixion.runtime

object DataIterables {
    @JvmStatic fun characters(value: String): Iterable<Char> = value.asIterable()
}
