package com.kingmang.ixion.compiler.ix_function.output;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class PrintlnFunction {
    /*
    Функция пойдет в scope.
    */
    public static final IxFunction PRINTLN_VOID =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("()V"));

    public static final IxFunction PRINTLN_DOUBLE =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(D)V")
            );
    public static final IxFunction PRINTLN_FLOAT =
            new IxFunction(
                    IxFunctionType.OUTPUT,
				    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(F)V")
            );
    public static final IxFunction PRINTLN_INTEGER =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(I)V")
            );
    public static final IxFunction PRINTLN_BOOLEAN =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(Z)V")
            );

    public static final IxFunction PRINTLN_CHAR =
            new IxFunction(IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(C)V")
            );
    public static final IxFunction PRINTLN_LONG =
            new IxFunction(IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType("(J)V")
            );

    public static final IxFunction PRINTLN_OBJECT =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "println",
                    "java/io/PrintStream",
                    IxType.getMethodType(IxType.VOID_TYPE, IxType.NULLABLE_OBJECT_TYPE)
            );
}

