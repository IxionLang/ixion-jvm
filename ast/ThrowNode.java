package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class ThrowNode implements Node {
	private final Token throwTok;
	private final Node throwee;

	public ThrowNode(Token throwTok, Node throwee) {
		this.throwTok = throwTok;
		this.throwee = throwee;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		IxType throweeType = throwee.getReturnType(context.getContext());

		if(throweeType.isPrimitive()) {
			throw new IxException(throwTok, "Cannot throw primitive type (got '%s').".formatted(throweeType));
		}

		try {
			if(!IxType.getObjectType("java/lang/Throwable").isAssignableFrom(throweeType, context.getContext(), false)) {
				throw new IxException(throwTok, "throw target must be an extension of java.lang.Throwable ('%s' cannot be cast).".formatted(throweeType));
			}
		} catch (ClassNotFoundException e) {
			throw new IxException(throwTok, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
		throwee.visit(context);
		context.getContext().getMethodVisitor().visitTypeInsn(Opcodes.CHECKCAST, throweeType.getInternalName());
		context.getContext().getMethodVisitor().visitInsn(Opcodes.ATHROW);

		context.getContext().getScope().setReturned(true);
	}

	@Override
	public String toString() {
		return "throw " + throwee + ";";
	}
}
