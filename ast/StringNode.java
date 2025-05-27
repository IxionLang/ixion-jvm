package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;

public class StringNode implements Node {
	private final Token value;

	public StringNode(Token value) {
		this.value = value;
	}

	@Override
	public void visit(FileContext context) throws IxException {
		context.getContext().updateLine(value.line());
		String val = value.value().substring(1, value.value().length() - 1);

		val = escape(val);

		context.getContext().getMethodVisitor().visitLdcInsn(val);
	}

	private String escape(String value) {
		StringBuilder builder = new StringBuilder();

		for(int i = 0; i < value.length(); i++) {
			if(value.charAt(i) == '\\') {
				i++;
				switch(value.charAt(i)) {
					case 't' -> builder.append('\t');
					case 'b' -> builder.append('\b');
					case 'n' -> builder.append('\n');
					case 'r' -> builder.append('\r');
					case 'f' -> builder.append('\f');
					case '\'' -> builder.append('\'');
					case '\"' -> builder.append('\"');
					default -> builder.append(value.charAt(i));
				}
			}
			else {
				builder.append(value.charAt(i));
			}
		}
		return builder.toString();
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return IxType.STRING_TYPE;
	}

	@Override
	public Object getConstantValue(Context context) {
		return escape(value.value().substring(1, value.value().length() - 1));
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
