package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class BooleanNode implements Node {
	private final Token value;
	private final boolean boolValue;

	public BooleanNode(Token value) {
		this.value = value;
		boolValue = Boolean.parseBoolean(value.value());
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().getMethodVisitor().visitInsn(boolValue ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.BOOLEAN_TYPE;
	}

	@Override
	public Object getConstantValue(Context context) {
		return boolValue;
	}

	@Override
	public boolean isConstant(Context context) {
		return true;
	}

	@Override
	public String toString() {
		return value.value();
	}
}
