package com.kingmang.ixion.runtime;

public interface IxType {

	Object getDefaultValue();

	String getDescriptor();

	String getInternalName();

	int getLoadVariableOpcode();

	String getName();

	int getReturnOpcode();

	Class<?> getTypeClass();

	boolean isNumeric();

	String kind();

}
