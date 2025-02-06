package ixion.std;

public class ternary {
    public static <T> T ternaryOperator(boolean condition, T trueValue, T falseValue) {
        return condition ? trueValue : falseValue;
    }
}
