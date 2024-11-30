package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;

import java.util.List;

public class TypeNode implements Node {
	private final Token root;
	private final String path;
	private final boolean isPrimitive;
	private final int dimensions;
	private final List<Integer> nullableDimensions;
	private final TypeNode element;
	private boolean isNullable;

	public TypeNode(Token value) {
		this.root = value;
		this.path = null;
		this.isPrimitive = true;
		this.dimensions = 0;
		this.nullableDimensions = null;
		this.element = null;
		this.isNullable = false;
	}

	public TypeNode(Token root, String path) {
		this.root = root;
		this.path = path;
		this.isPrimitive = false;
		this.dimensions = 0;
		this.nullableDimensions = null;
		this.element = null;
		this.isNullable = false;
	}

	public TypeNode(TypeNode element, int dimensions, List<Integer> nullableDimensions) {
		this.root = element.root;
		this.path = null;
		this.isPrimitive = false;
		this.dimensions = dimensions;
		this.nullableDimensions = nullableDimensions;
		this.element = element;
		this.isNullable = false;
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		if(isNullable && isPrimitive) {
			throw new IxException(root, "Primitive types cannot be nullable.");
		}

		if(dimensions != 0) return IxType.getArrayType(element.getReturnType(context), dimensions, nullableDimensions).asNullable(isNullable);

		if(isPrimitive) return switch (root.type()) {
			case VOID -> IxType.VOID_TYPE;
			case INT -> IxType.INT_TYPE;
			case DOUBLE -> IxType.DOUBLE_TYPE;
			case FLOAT -> IxType.FLOAT_TYPE;
			case BOOLEAN -> IxType.BOOLEAN_TYPE;
			case CHAR -> IxType.CHAR_TYPE;
			case LONG -> IxType.LONG_TYPE;
			case BYTE -> IxType.BYTE_TYPE;
			case SHORT -> IxType.SHORT_TYPE;
			default -> null;
		};

		try {
			return IxType.getType(TypeUtil.classForName(path, context)).asNullable(isNullable);
		} catch (ClassNotFoundException e) {
			throw new IxException(root, "Could not resolve class '%s'".formatted(e.getMessage()));
		}
	}

	public IxType getRawClassType() throws IxException {
		if(isPrimitive) throw new IxException(root, "Unexpected primitive type");

		return IxType.getObjectType(path.replace('.', '/')).asNullable(isNullable);
	}

	public void setNullable(boolean isNullable) {
		this.isNullable = isNullable;
	}

	@Override
	public void visit(FileContext context) throws IxException {
	}

	@Override
	public String toString() {
		if(dimensions != 0) {
			StringBuilder builder = new StringBuilder(element.toString());
			for(int i = 0; i < dimensions; i++) {
				if(nullableDimensions != null && nullableDimensions.contains(i)) builder.append('?');
				builder.append("[]");
			}
			if(isNullable) builder.append('?');
			return builder.toString();
		}

		return isPrimitive ? root.value() : path + (isNullable ? "?" : "");
	}
}
