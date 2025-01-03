package com.kingmang.ixion.api.libs_api;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class IxLibraryImpl {
    //constants
    private static final String defaultPath = "ixion/lang/";
    private static final String defaultJavaPath = "Ljava/lang/";
    public static final String INT = "I";
    public static final String FLOAT = "F";
    public static final String INTEGER_CLASS = defaultJavaPath.concat("Integer;");
    public static final String OBJECT = defaultJavaPath.concat("Object;");
    public static final String FLOAT_CLASS = defaultJavaPath.concat("Float;");

    //статичная функция языка, нужно добавлять в scope
    public static IxFunction function(String name, String className, String arg, String returnedType){

        return new IxFunction(
                IxFunctionType.STATIC,
                name,
                defaultPath.concat(className),
                IxType.getMethodType("(".concat(arg).concat(")").concat(returnedType))
        );
    }
}
