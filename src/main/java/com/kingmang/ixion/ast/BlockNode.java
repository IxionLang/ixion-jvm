package com.kingmang.ixion.ast;

import com.kingmang.ixion.compiler.Scope;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;

import java.util.List;
import java.util.stream.Collectors;


public class BlockNode implements Node {

	private final List<Node> body;

	public BlockNode(List<Node> body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "{" + body.stream().map(Node::toString).collect(Collectors.joining()) + "}";
	}

	@Override
	public void visit(FileContext context) throws IxException {
		Scope outer = context.getContext().getScope();

		context.getContext().setScope(outer.nextDepth());
		for(Node n : body) {
			n.visit(context);
		}
		outer.setReturned(context.getContext().getScope().isReturned());
		context.getContext().setScope(outer);
	}
}
