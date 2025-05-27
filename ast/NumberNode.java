package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.TypeUtil;
import com.kingmang.ixion.types.IxType;

public class NumberNode implements Node {
	private final Token value;
	private final IxType type;

	public NumberNode(Token value) {
		this.value = value;
		type = computeCorrectType();
	}

	@Override
	public String toString() {
		return value.value();
	}

	@Override
	public void visit(FileContext context) {
		context.getContext().updateLine(value.line());
		if(type.equals(IxType.INT_TYPE)) {
			TypeUtil.generateCorrectInt(Integer.parseInt(value.value()), context.getContext());
		}
		else if(type.equals(IxType.DOUBLE_TYPE)) {
			TypeUtil.generateCorrectDouble(Double.parseDouble(value.value()), context.getContext());
		}
		else if(type.equals(IxType.FLOAT_TYPE)) {
			TypeUtil.generateCorrectFloat(Float.parseFloat(value.value()), context.getContext());
		}
		else if(type.equals(IxType.LONG_TYPE)) {
			TypeUtil.generateCorrectLong(Long.parseLong(value.value().substring(0, value.value().length() - 1)), context.getContext());
		}
	}

	@Override
	public IxType getReturnType(Context context) {
		return computeCorrectType();
	}

	@Override
	public Object getConstantValue(Context context) {
		if(type.equals(IxType.INT_TYPE)) return Integer.parseInt(value.value());
		else if(type.equals(IxType.DOUBLE_TYPE)) return Double.parseDouble(value.value());
		else if(type.equals(IxType.FLOAT_TYPE)) return Float.parseFloat(value.value());
		else if(type.equals(IxType.LONG_TYPE)) return Long.parseLong(value.value().substring(0, value.value().length() - 1));

		return null;
	}

	@Override
	public boolean isConstant(Context context) {
		return true;
	}

	private IxType computeCorrectType() {
		String rep = value.value();
		if(rep.endsWith("l") || rep.endsWith("L")) return IxType.LONG_TYPE;
		if(rep.endsWith("f") || rep.endsWith("F")) return IxType.FLOAT_TYPE;
		if(rep.contains(".")) return IxType.DOUBLE_TYPE;
		return IxType.INT_TYPE;
	}
}
