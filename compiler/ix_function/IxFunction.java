package com.kingmang.ixion.compiler.ix_function;

import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;


public record IxFunction(
		IxFunctionType functionType,
		String name,
		String parent,
		IxType type) {

	public int getAccess() {
		return switch (functionType) {
			case STATIC -> Opcodes.INVOKESTATIC;
			case CLASS, OUTPUT -> Opcodes.INVOKEVIRTUAL;
		};
	}
}
