package com.kingmang.ixion.runtime;

import static org.objectweb.asm.Opcodes.*;

public enum TypeSpecificOpcodes {

	INT(ILOAD, ISTORE, IRETURN, IADD, ISUB, IMUL, IDIV, INEG),
	FLOAT(FLOAD, FSTORE, FRETURN, FADD, FSUB, FMUL, FDIV, FNEG),
	DOUBLE(DLOAD, DSTORE, DRETURN, DADD, DSUB, DMUL, DDIV, DNEG),
	VOID(ALOAD, ASTORE, RETURN, 0, 0, 0, 0, 0),
	OBJECT(ALOAD, ASTORE, ARETURN, 0, 0, 0, 0, 0);

	private final int load;
	private final int store;
	private final int ret;
	private final int add;
	private final int sub;
	private final int mul;
	private final int div;
	private final int neg;

	TypeSpecificOpcodes(int load, int store, int ret, int add, int sub, int mul, int div, int neg) {

		this.load = load;
		this.store = store;
		this.ret = ret;
		this.add = add;
		this.sub = sub;
		this.mul = mul;
		this.div = div;
		this.neg = neg;
	}

	public int getAdd() {
		return add;
	}

	public int getDivide() {
		return div;
	}

	public int getLoad() {
		return load;
	}

	public int getMultiply() {
		return mul;
	}

	public int getNeg() {
		return neg;
	}

	public int getReturn() {
		return ret;
	}

	public int getStore() {
		return store;
	}

	public int getSubtract() {
		return sub;
	}

}