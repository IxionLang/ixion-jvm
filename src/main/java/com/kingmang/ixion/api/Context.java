package com.kingmang.ixion.api;

import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.runtime.IxType;
import org.apache.commons.collections4.map.LinkedMap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Context {

	private final LinkedMap<String, IxType> variables = new LinkedMap<>();

	private final Map<String, IxionConstant.Mutability> mutability = new HashMap<>();
	private Context parent;

	public Context() {
		this.parent = null;
	}

	public void addVariable(String name, IxType type) {
		variables.put(name, type);
		mutability.put(name, IxionConstant.Mutability.IMMUTABLE);
	}

	public void addVariableOrError(IxApi ixApi, String name, IxType type, File file, Node node) {
		if (getVariable(name) != null) {
			new RedeclarationException().send(ixApi, file, node, name);
		} else {
			addVariable(name, type);
		}
	}

	public Context getParent() {
		return parent;
	}

	public void setParent(Context parent) {
		this.parent = parent;
	}

	public IxType getVariable(String name) {
		if (variables.get(name) != null) {
			return variables.get(name);
		}
		if (parent != null) {
			return parent.getVariable(name);
		}
		return null;
	}

	public IxionConstant.Mutability getVariableMutability(String name) {
		if (mutability.get(name) != null) {
			return mutability.get(name);
		}
		if (parent != null) {
			return parent.getVariableMutability(name);
		}
		return null;
	}

	public <T> T getVariableTyped(String name, Class<T> clazz) {
		var v = getVariable(name);
		if (clazz.isInstance(v)) {
			return (T) v;
		}
		return null;
	}

	public void setVariableMutability(String name, IxionConstant.Mutability m) {
		if (mutability.get(name) != null) {
			mutability.put(name, m);
		}

	}

	public void setVariableType(String name, IxType type) {
		if (variables.get(name) != null) {
			variables.put(name, type);
		}
	}

}
