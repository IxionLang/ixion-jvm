package com.kingmang.ixion.lexer;

import org.apache.commons.text.StringEscapeUtils;

public record Token(TokenType type, int line, int col, String source) {

	public String representation() {
		return type.representation;
	}

	@Override
	public String toString() {
		return type.name() + "{" + StringEscapeUtils.escapeJava(source) + "}@" + line + ":" + col;
	}

}