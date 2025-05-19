package com.kingmang.ixion.ast;

import com.kingmang.ixion.util.FileContext;
import com.kingmang.ixion.exceptions.IxException;
import com.kingmang.ixion.parser.Value;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.compiler.Context;
import com.kingmang.ixion.types.IxType;
import lombok.Getter;

@Getter
public class GroupingNode implements Node {
	private final Node value;

	public GroupingNode(Node value) {
		this.value = value;
	}

	@Override
	public IxType getReturnType(Context context) throws IxException {
		return value.getReturnType(context);
	}

	@Override
	public Object getConstantValue(Context context) throws IxException {
		return value.getConstantValue(context);
	}

	@Override
	public boolean isConstant(Context context) throws IxException {
		return value.isConstant(context);
	}

	@Override
	public void visit(FileContext context) throws IxException {
		value.visit(context);
	}

	@Override
	public void preprocess(Context context) throws IxException {
		value.preprocess(context);
	}

	@Override
	public Value getLValue() {
		return value.getLValue();
	}

	@Override
	public Object[] getLValueData() {
		return value.getLValueData();
	}

	@Override
	public String toString() {
		return "(%s)".formatted(value.toString());
	}
}
