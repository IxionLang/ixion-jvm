package com.kingmang.ixion.api

object Debugger {
    var DEBUG: Boolean = false
    private const val BLUE_START = "\u001B[34m"
    private const val RESET = "\u001B[0m"

    @JvmStatic
    fun debug(message: String) {
        if (DEBUG) {
            println(BLUE_START + message + RESET)
        }
    }
}
