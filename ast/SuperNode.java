package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class SuperNode implements Node {

	private final Token superTok;

	public SuperNode(Token superTok) {
		this.superTok = superTok;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().updateLine(superTok.line());
		context.getContext().getMethodVisitor().visitVarInsn(Opcodes.ALOAD, 0);
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		if(context.getCurrentSuperClass() == null) {
			throw new IxException(superTok, "Can only use 'super' within a class.");
		}
		return context.getCurrentSuperClass();
	}

	@Override
	public String toString() {
		return "super";
	}
}
