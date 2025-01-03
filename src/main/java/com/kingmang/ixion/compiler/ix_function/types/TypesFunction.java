package com.kingmang.ixion.compiler.ix_function.types;

import com.kingmang.ixion.api.libs_api.IxLibraryImpl;
import com.kingmang.ixion.compiler.ix_function.IxFunction;
import com.kingmang.ixion.compiler.ix_function.IxFunctionType;
import com.kingmang.ixion.types.IxType;

public class TypesFunction {

    public static final IxFunction INT_BOXED =
            IxLibraryImpl.function(
                    "Int",
                    "Types",
                    IxLibraryImpl.INT,
                    IxLibraryImpl.INTEGER_CLASS
            );


    public static final IxFunction FLOAT_BOXED =
            IxLibraryImpl.function(
                    "Float",
                    "Types",
                    IxLibraryImpl.FLOAT,
                    IxLibraryImpl.FLOAT_CLASS
            );


    public static final IxFunction TOINT =
            IxLibraryImpl.function(
              "toInt",
              "Types",
              IxLibraryImpl.OBJECT,
              IxLibraryImpl.INT
            );

    public static final IxFunction TOFLOAT =
            IxLibraryImpl.function(
                    "toFloat",
                    "Types",
                    IxLibraryImpl.OBJECT,
                    IxLibraryImpl.FLOAT
            );
}
