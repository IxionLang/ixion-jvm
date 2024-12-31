package com.kingmang.ixion.compiler.ix_function.types;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class TypesFunction {
    public static final IxFunction INT =
            new IxFunction(
                    IxFunctionType.STATIC,
                    "Int",
                    "ixion/lang/Types",
                    IxType.getMethodType("(I)Ljava/lang/Integer;"));

    public static final IxFunction FLOAT =
            new IxFunction(
                    IxFunctionType.STATIC,
                    "Float",
                    "ixion/lang/Types",
                    IxType.getMethodType("(F)Ljava/lang/Float;"));


    public static final IxFunction TOINT =
            new IxFunction(
                    IxFunctionType.STATIC,
                    "toInt",
                    "ixion/lang/Types",
                    IxType.getMethodType("(Ljava/lang/Object;)I"));

    public static final IxFunction TOFLOAT =
            new IxFunction(
                    IxFunctionType.STATIC,
                    "toFloat",
                    "ixion/lang/Types",
                    IxType.getMethodType("(Ljava/lang/Object;)F"));
}
