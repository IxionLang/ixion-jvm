package com.kingmang.ixion.runtime;

import org.objectweb.asm.Opcodes;

import java.util.Objects;

public record ListType(IxType contentType) implements IxType {

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ListType listType = (ListType) o;
		return contentType.equals(listType.contentType());
	}


	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String getDescriptor() {
		return "Ljava/util/List;";
	}


	@Override
	public String getInternalName() {
		return null;
	}

	@Override
	public int getLoadVariableOpcode() {
		return Opcodes.ALOAD;
	}


	@Override
	public String getName() {
		return contentType + "[]";
	}


	@Override
	public int getReturnOpcode() {
		return Opcodes.ARETURN;
	}


	@Override
	public Class<?> getTypeClass() {
		return java.util.List.class;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType);
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public String kind() {
		return "list";
	}

	@Override
	public String toString() {
		return getName();
	}
}
