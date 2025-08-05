package com.kingmang.ixion.api.libs_api;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class IxLibraryImpl {
    //constants
    private static final String RUNTIME_PATH = "com/kingmang/ixion/runtime/";
    private static final String DEFAULT_JAVA_PATH = "Ljava/lang/";

    public static final String INT = "I";
    public static final String CHAR = "C";
    public static final String FLOAT = "F";

    public static final String STRING_CLASS = DEFAULT_JAVA_PATH.concat("String;");
    public static final String INTEGER_CLASS = DEFAULT_JAVA_PATH.concat("Integer;");
    public static final String OBJECT = DEFAULT_JAVA_PATH.concat("Object;");
    public static final String FLOAT_CLASS = DEFAULT_JAVA_PATH.concat("Float;");

    public static IxFunction function(String name, String className, String arg, String returnedType){
        return new IxFunction(
                IxFunctionType.STATIC,
                name,
                RUNTIME_PATH.concat(className),
                IxType.getMethodType("(".concat(arg).concat(")").concat(returnedType))
        );
    }
}
