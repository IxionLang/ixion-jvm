package com.kingmang.ixion.compiler;

import com.kingmang.ixion.types.IxType;
import lombok.Getter;
import lombok.Setter;


public class Variable {

	@Getter
    private final String name;

	@Getter
    private final String owner;

	@Setter
    @Getter
    private IxType type;

	@Getter
    private final VariableType variableType;

	@Getter
    private final int index;

	private final boolean isConst;

	public Variable(VariableType variableType, String name, String owner, IxType type, boolean isConst) {
		this.name = name;
		this.owner = owner;
		this.type = type;
		this.variableType = variableType;
		this.isConst = isConst;
		this.index = 0;
	}

	public Variable(VariableType variableType, String name, int index, IxType type, boolean isConst) {
		this.variableType = variableType;
		this.index = index;
		this.type = type;
		this.name = name;
		this.isConst = isConst;
		this.owner = null;
	}

    public boolean isConst() {
		return isConst;
	}

}
