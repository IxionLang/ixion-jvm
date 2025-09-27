package com.kingmang.ixion.parser;

import java.util.HashSet;
import java.util.Set;

public class ReservedWords {
	static final Set<String> reserved = new HashSet<>();

	static {
		reserved.add("int");
		reserved.add("float");
		reserved.add("bool");
		reserved.add("string");
		reserved.add("def");
		reserved.add("struct");
	}

	public static boolean isReserved(String word) {
		return reserved.contains(word);
	}
}
