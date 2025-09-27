package com.kingmang.ixion.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MonomorphizedStruct implements IxType {
	public final StructType struct;
	public final Map<String, IxType> resolved = new HashMap<>();

	public MonomorphizedStruct(StructType struct) {
		this.struct = struct;
	}

	@Override
	public Object getDefaultValue() {
		return struct.getDefaultValue();
	}

	@Override
	public String getDescriptor() {
		return struct.getDescriptor();
	}

	@Override
	public String getInternalName() {
		return struct.getInternalName();
	}

	@Override
	public int getLoadVariableOpcode() {
		return struct.getLoadVariableOpcode();
	}

	@Override
	public String getName() {
		return struct.getName();
	}

	@Override
	public int getReturnOpcode() {
		return struct.getReturnOpcode();
	}

	@Override
	public Class<?> getTypeClass() {
		return struct.getTypeClass();
	}

	@Override
	public boolean isNumeric() {
		return struct.isNumeric();
	}

	@Override
	public String kind() {
		return struct.kind();
	}

	@Override
	public String toString() {
		return "type " + getName() + " = struct" + "[" + struct.generics.stream().map(g -> g + "=" + resolved.get(g)).collect(Collectors.joining(",")) + "]";
	}
}
