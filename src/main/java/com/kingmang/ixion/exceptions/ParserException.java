package com.kingmang.ixion.exceptions;

import com.kingmang.ixion.parser.tokens.Token;

public class ParserException extends Exception {
	private final Token location;
	private final String message;

    public ParserException(Token location, String message) {
		super(message);
		this.location = location;
		this.message = message;
	}

	public String defaultError(String filename) {
        String parserPattern = """
                ┌──────────────────────Parser Exception────────────────────
                │[%s:%s] Unexpected token in file "%s" {'%s'}:
                │%s
                ╰──────────────────────────────────────────────────────────
                """;
        return parserPattern.formatted(location.line(), location.column(), filename, location.value(), message);
	}

}
