package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class ToNode implements Node {
	private final Node left;
	private final Node type;
	private final Token as;

	public ToNode(Node left, Node type, Token as) {
		this.left = left;
		this.type = type;
		this.as = as;
	}

	@Override
	public void visit(FileContext fc) throws IxException {
		Context context = fc.getContext();
		IxType from = left.getReturnType(context);
		IxType to = type.getReturnType(context);

		try {
			if(from.toClass(context).equals(to.toClass(context))) return;
		} catch (ClassNotFoundException e) {
			throw new IxException(as, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		if(from.equals(IxType.VOID_TYPE)) throw new IxException(as, "Cannot cast from void");

		if(from.isPrimitive() && to.isPrimitive()) {
			left.visit(fc);
			from.cast(to, context.getMethodVisitor());
		}
		else if(!from.isPrimitive() && !to.isPrimitive()) {
			left.visit(fc);
			context.getMethodVisitor().visitTypeInsn(Opcodes.CHECKCAST, to.getInternalName());

			try {
				if(!to.toClass(context).isAssignableFrom(from.toClass(context))
				&& !from.toClass(context).isAssignableFrom(to.toClass(context)))
					throw new IxException(as, "Cannot cast type '%s' to '%s'".formatted(from.getClassName(), to.getClassName()));
			} catch (ClassNotFoundException e) {
				throw new IxException(as, "Could not resolve class '%s'".formatted(e.getMessage()));
			}
		}
		else {
			if(from.equals(IxType.STRING_TYPE)) {
				stringCast(to, fc);
			}
			else throw new IxException(as, "Cannot cast between objects and primitives ('%s' to '%s')".formatted(from.getClassName(), to.getClassName()));
		}
	}

	private void stringCast(IxType to, FileContext fc) throws IxException {
		switch (to.getSort()) {
			case INT -> {
				left.visit(fc);
				fc.getContext().getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			}
			case DOUBLE -> {
				left.visit(fc);
				fc.getContext().getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "parseDouble", "(Ljava/lang/String;)D", false);
			}
			case BOOLEAN -> {
				left.visit(fc);
				fc.getContext().getMethodVisitor().visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
			}
			default -> throw new IxException(as, "Cannot cast between objects and primitives ('java.lang.String' to '%s')".formatted(to.getClassName()));
		}
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return type.getReturnType(context);
	}

	@Override
	public String toString() {
		return left.toString() + " to " + type.toString();
	}
}
