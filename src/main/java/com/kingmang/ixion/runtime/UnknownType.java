package com.kingmang.ixion.runtime;

import org.objectweb.asm.Opcodes;

import java.io.Serializable;

public class UnknownType implements IxType, Serializable {
	static int counter = 1;
	public final String typeName;

	public UnknownType(String typeName) {
		this.typeName = typeName;
		counter++;
	}

	public UnknownType() {
		this.typeName = "";
		counter++;
	}


	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String getDescriptor() {
		return "L" + getInternalName() + ";";
	}


	@Override
	public String getInternalName() {
		return getName().replace(".", "/");
	}

	@Override
	public int getLoadVariableOpcode() {
		throw new RuntimeException("ALOAD on UnknownType");
	}


	@Override
	public String getName() {
		return typeName.length() > 0 ? typeName : "<unknown>";
	}


	@Override
	public int getReturnOpcode() {
		return Opcodes.ARETURN;
	}


	@Override
	public Class<?> getTypeClass() {
		return null;
	}

	@Override
	public boolean isNumeric() {
		return false;
	}

	@Override
	public String kind() {
		return null;
	}

	@Override
	public String toString() {
		return "$" + getName();
	}
}
