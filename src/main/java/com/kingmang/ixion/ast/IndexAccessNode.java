package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.types.IxType;
import org.objectweb.asm.Opcodes;

public class IndexAccessNode implements Node {
	private final Token bracket;
	private final Node left;
	private final Node index;

	public IndexAccessNode(Token bracket, Node left, Node index) {
		this.bracket = bracket;
		this.left = left;
		this.index = index;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		left.visit(context);

		IxType returnType = left.getReturnType(context.getContext());
		if(returnType.isNullable()) {
			throw new IxException(bracket, "Cannot use '[' to call methods on a nullable type ('%s')".formatted(returnType));
		}

		visitAccess(context);
	}

	public void visitAccess(FileContext context) throws IxException {
		IxType leftType = left.getReturnType(context.getContext());
		IxType indexType = index.getReturnType(context.getContext());

		if(!leftType.isArray()) {
			throw new IxException(bracket, "Cannot get index of type '%s'".formatted(leftType));
		}
		if(!indexType.isInteger()) {
			throw new IxException(bracket, "Index must be an integer type (got '%s')".formatted(indexType));
		}

		index.visit(context);

		context.getContext().getMethodVisitor().visitInsn(leftType.getElementType().getOpcode(Opcodes.IALOAD));
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		IxType leftType = left.getReturnType(context);

		if(!leftType.isArray()) {
			throw new IxException(bracket, "Cannot get index of type '%s'".formatted(leftType));
		}

		return leftType.getElementType();
	}

	@Override
	public Value getLValue() {
		return Value.ARRAY;
	}

	@Override
	public Object[] getLValueData() {
		return new Object[] { left, index, bracket };
	}

	@Override
	public String toString() {
		return "%s[%s]".formatted(left, index);
	}
}
