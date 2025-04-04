package com.kingmang.ixion.api.libs_api;

import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class IxLibraryImpl {
    //constants
    private static String runtimePath = "com/kingmang/ixion/runtime/";
    private static final String defaultJavaPath = "Ljava/lang/";

    public static final String INT = "I";
    public static final String CHAR = "C";
    public static final String FLOAT = "F";

    public static final String STRING_CLASS = defaultJavaPath.concat("String;");
    public static final String INTEGER_CLASS = defaultJavaPath.concat("Integer;");
    public static final String OBJECT = defaultJavaPath.concat("Object;");
    public static final String FLOAT_CLASS = defaultJavaPath.concat("Float;");

    public static IxFunction function(String name, String className, String arg, String returnedType){
        //if(className.contains("java")) runtimePath = "";
        return new IxFunction(
                IxFunctionType.STATIC,
                name,
                runtimePath.concat(className),
                IxType.getMethodType("(".concat(arg).concat(")").concat(returnedType))
        );
    }
}
