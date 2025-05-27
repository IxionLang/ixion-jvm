package com.kingmang.ixion.parser;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.types.IxType;


public interface Node {

	void visit(FileContext context) throws IxException;
	default void preprocess(Context context) throws IxException { }
	default IxType getReturnType(Context context) throws IxException { return IxType.VOID_TYPE; }
	default Object getConstantValue(Context context) throws IxException { return null; }
	default boolean isConstant(Context context) throws IxException { return false; }
	default Value getLValue() { return Value.NONE; }
	default Object[] getLValueData() { return new Object[0]; }
	default boolean isNewClass() { return false; }
	default void buildClasses(Context context) throws IxException {}
}
