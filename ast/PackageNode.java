package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.types.IxType;

public class PackageNode implements Node {
	private final Token packageNode;
	private final TypeNode name;

	public PackageNode(Token packageNode, TypeNode name) {
		this.packageNode = packageNode;
		this.name = name;
	}

	@Override
	public void visit(FileContext context) throws IxException {
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return name.getRawClassType();
	}

	@Override
	public String toString() {
		return "package %s;".formatted(name);
	}
}
