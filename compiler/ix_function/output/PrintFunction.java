package com.kingmang.ixion.compiler.ix_function.output;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class PrintFunction {
    /*
    То же самое, что и println.
    */
    public static final IxFunction PRINT_VOID =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("()V"));

    public static final IxFunction PRINT_DOUBLE =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(D)V")
            );
    public static final IxFunction PRINT_FLOAT =
            new IxFunction(
                    IxFunctionType.OUTPUT,
				    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(F)V")
            );
    public static final IxFunction PRINT_INTEGER =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(I)V")
            );
    public static final IxFunction PRINT_BOOLEAN =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(Z)V")
            );

    public static final IxFunction PRINT_CHAR =
            new IxFunction(IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(C)V")
            );
    public static final IxFunction PRINT_LONG =
            new IxFunction(IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType("(J)V")
            );

    public static final IxFunction PRINT_OBJECT =
            new IxFunction(
                    IxFunctionType.OUTPUT,
                    "print",
                    "java/io/PrintStream",
                    IxType.getMethodType(IxType.VOID_TYPE, IxType.NULLABLE_OBJECT_TYPE)
            );
}

