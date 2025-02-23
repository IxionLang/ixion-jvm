package com.kingmang.ixion.exceptions;

import com.kingmang.ixion.parser.tokens.Token;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IxException extends Exception {

	private final Token location;
	private final String message;
	private final String IxionPattern = """
			[Ixion Exception]
			│> [%s:%s] in file "%s" ['%s']:
			│> %s
			""";

	public IxException(Token location, String message) {
		super(message);
		this.location = location;
		this.message = message;
	}

	public String defaultError(String filename) {
		if(location == null) return IxionPattern.formatted(-1, -1, filename, "not found token location", message);
		return IxionPattern.formatted(location.line(), location.column(), filename, location.value(), message);
	}


}
