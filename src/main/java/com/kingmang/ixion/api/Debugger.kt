package com.kingmang.ixion.api;

public class Debugger {
    public static boolean DEBUG = false;
    private static final String BLUE_START = "\u001B[34m";
    private static final String RESET = "\u001B[0m";

    public static void debug(String message) {
        if (DEBUG) {
            System.out.println(BLUE_START.concat(message).concat(RESET));
        }
    }
}
