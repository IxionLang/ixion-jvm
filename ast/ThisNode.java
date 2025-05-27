package com.kingmang.ixion.ast;

import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class ThisNode implements Node {

	private final Token thisTok;

	public ThisNode(Token thisTok) {
		this.thisTok = thisTok;
	}

	@Override
	public void visit(FileContext context) throws IxException {

		if(context.getContext().isStaticMethod()) {
			throw new IxException(thisTok, "Cannot access 'this' in a static context");
		}

		context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.getObjectType(context.getCurrentClass());
	}

	@Override
	public String toString() {
		return "this";
	}
}
