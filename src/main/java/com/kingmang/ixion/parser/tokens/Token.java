package com.kingmang.ixion.parser.tokens;


public record Token(
		TokenType type,
		String value,
		int line,
		int column
) {

	public String toString() {
		return "[%d:%d] %s: %s".formatted(line, column, type, value);
	}
}
