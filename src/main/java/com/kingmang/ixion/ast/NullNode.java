package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class NullNode implements Node {
	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().getMethodVisitor().visitInsn(Opcodes.ACONST_NULL);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.NULL_TYPE;
	}

	@Override
	public Object getConstantValue(Context context) {
		return null;
	}

	@Override
	public boolean isConstant(Context context) throws IxException {
		return true;
	}

	@Override
	public String toString() {
		return "null";
	}
}
