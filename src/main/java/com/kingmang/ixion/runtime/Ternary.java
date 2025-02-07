package com.kingmang.ixion.runtime;

public class Ternary {
    public static <T> T ternaryOperator(boolean condition, T trueValue, T falseValue) {
        return condition ? trueValue : falseValue;
    }
}
