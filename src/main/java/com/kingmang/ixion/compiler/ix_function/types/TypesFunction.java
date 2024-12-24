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
                    IxType.getMethodType("(I)Ljava/lang/Float;"));
}
