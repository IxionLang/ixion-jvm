package com.kingmang.ixion.exception


class Panic(private val message: String) {
    fun send() {
        println(R + ("panic: " + message) + RESET)
        System.exit(1)
    }

    companion object {
        private const val R = "\u001B[31m"
        private const val RESET = "\u001B[0m"
    }
}
