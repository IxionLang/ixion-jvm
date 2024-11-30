package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReturnNode implements Node {
	private final Token returnTok;
	private final Node expression;

	public ReturnNode(Token returnTok, Node expression) {
		this.returnTok = returnTok;
		this.expression = expression;
	}


	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().updateLine(returnTok.line());
		MethodVisitor mv = context.getContext().getMethodVisitor();
		Scope scope = context.getContext().getScope();

		if(expression == null) {
			if(!scope.getReturnType().equals(IxType.VOID_TYPE)) throw new IxException(returnTok, "Non-void function's return must have a value.");
			mv.visitInsn(Opcodes.RETURN);
			scope.setReturned(true);
		}
		else {
			IxType returnType = expression.getReturnType(context.getContext());

			if(returnType.equals(IxType.VOID_TYPE)) throw new IxException(returnTok, "Cannot return void value");

			if(scope.getReturnType().equals(IxType.VOID_TYPE)) throw new IxException(returnTok, "Cannot return value from void function");

			expression.visit(context);

			try {
				if(!scope.getReturnType().isAssignableFrom(returnType, context.getContext(), true)) {
					throw new IxException(returnTok,
							"Cannot return type '%s' from function expecting '%s'"
									.formatted(returnType, scope.getReturnType()));
				}
			} catch (ClassNotFoundException e) {
				throw new IxException(returnTok, "Could not resolve class '%s'".formatted(e.getMessage()));
			}

			mv.visitInsn(scope.getReturnType().getOpcode(Opcodes.IRETURN));
			scope.setReturned(true);
		}

	}

	@Override
	public String toString() {
		return "return" + (expression == null ? "" : (" " + expression.toString())) + ";";
	}
}
