package com.kingmang.ixion.runtime;

import org.objectweb.asm.Opcodes;

public record ExternalType(Class<?> foundClass) implements IxType {

	@Override
	public Object getDefaultValue() {
		return null;
	}

	@Override
	public String getDescriptor() {
		return foundClass.descriptorString();
	}


	@Override
	public String getInternalName() {
		return getName().replace(".", "/");
	}

	@Override
	public int getLoadVariableOpcode() {
		return Opcodes.ALOAD;
	}


	@Override
	public String getName() {
		return foundClass.getName();
	}


	@Override
	public int getReturnOpcode() {
		return Opcodes.ARETURN;
	}


	@Override
	public Class<?> getTypeClass() {
		return foundClass;
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
		return getName();
	}
}
