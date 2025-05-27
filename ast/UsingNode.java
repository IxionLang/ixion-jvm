package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.types.IxType;

public record UsingNode(Token usingTok, Node type, Token as) implements Node {

	@Override
	public void visit(FileContext context) throws IxException {
	}

	@Override
	public void preprocess(Context context) throws IxException {
		IxType importType = type.getReturnType(context);

		if (importType.isPrimitive()) {
			throw new IxException(usingTok, "Cannot import primitive type");
		}

		Class<?> klass;
		try {
			klass = Class.forName(importType.getClassName(), false, context.getLoader());
		} catch (ClassNotFoundException e) {
			throw new IxException(usingTok, "Could not resolve class '%s'".formatted(e.getMessage()));
		}

		String name = as == null ? klass.getSimpleName() : as.value();
		context.getUsings().put(name, importType.getClassName());
	}

	@Override
	public String toString() {
		return "using %s%s;".formatted(type,
				as == null ? "" : (" as " + as.value()));
	}
}
