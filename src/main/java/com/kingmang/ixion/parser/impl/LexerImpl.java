package com.kingmang.ixion.parser.impl;

import com.kingmang.ixion.parser.Lexer;
import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;

import java.util.ArrayList;
import java.util.List;


public class LexerImpl implements Lexer {

	public static List<TokenType> PRIMITIVE_TYPES = List.of(
			TokenType.INT,
			TokenType.DOUBLE,
			TokenType.FLOAT,
			TokenType.BOOLEAN,
			TokenType.CHAR,
			TokenType.LONG,
			TokenType.BYTE,
			TokenType.SHORT
	);

	private int index;
	private int line;
	private int column;
	private int start;
	private char current;
	private final String text;

	public LexerImpl(String text){
		this.text = text;
		this.start = 0;
		this.index = 0;
		this.line = 1;
		this.column = 0;
		this.current = text.charAt(0);
	}

	@Override
	public List<Token> tokenize() {
		ArrayList<Token> tokens = new ArrayList<>();

		while(!isAtEnd()) {
			start = index;
			advance();
			Token token;
			if(isWhitespace(current)) {
				if (current == '\n') {
					line++;
					column = 0;
				}
				continue;
			}
			else if(current == '/' && (!isAtEnd() && text.charAt(index) == '/')) {
				do {
					advance();
				} while(!isAtEnd() && current != '\n');
				line++;
				continue;
			}
			else if(current == '/' && (!isAtEnd() && text.charAt(index) == '*')) {
				while(true) {
					if(current == '*' && (!isAtEnd() && text.charAt(index) == '/')) {
						advance();
						break;
					}
					advance();
					if(current == '\n') line++;
				}
				continue;
			}
			else if(isValidIdentifierStart(current)) {
				token = identifier();
			}
			else if(isNumeric(current)) {
				token = number();
			}
			else if(current == '"') {
				token = string();
			}
			else if(current == '\'') {
				token = character();
			}
			else {
				TokenType type = switch (current) {
					case '{' -> TokenType.LBRACE;
					case '@' -> TokenType.AT;
					case '}' -> TokenType.RBRACE;
					case '(' -> TokenType.LPAREN;
					case ')' -> TokenType.RPAREN;
					case '[' -> TokenType.LSQBR;
					case ']' -> TokenType.RSQBR;
					case ';' -> TokenType.SEMI;
					case ',' -> TokenType.COMMA;
					case '+' -> next('=') ? TokenType.IN_PLUS : next('+') ? TokenType.INCREMENT : TokenType.PLUS;
					case '-' -> next('>') ? TokenType.ARROW : next('=') ? TokenType.IN_MINUS : next('-') ? TokenType.DECREMENT : TokenType.MINUS;
					case '*' -> next('=') ? TokenType.IN_MUL : TokenType.STAR;
					case '/' -> next('=') ? TokenType.IN_DIV : TokenType.SLASH;
					case '%' -> next('=') ? TokenType.IN_MOD : TokenType.PERCENT;
					case '=' -> next('>') ? TokenType.LAMBDA : next('=') ? (next('=') ? TokenType.TRI_EQ : TokenType.EQEQ) : TokenType.EQUALS;
					case ':' -> TokenType.COLON;
					case '.' -> TokenType.DOT;
					case '!' -> next('=') ? (next('=') ? TokenType.TRI_EXEQ : TokenType.EXEQ) : TokenType.EXCLAIM;
					case '<' -> next('=') ? TokenType.LESS_EQ : next('<') ? TokenType.BITWISE_SHL : TokenType.LESS;
					case '>' -> lexGreaterThan();
					case '&' -> next('&') ? TokenType.LOGICAL_AND : next('=') ? TokenType.IN_BITWISE_AND : TokenType.BITWISE_AND;
					case '|' -> next('|') ? TokenType.LOGICAL_OR : next('=') ? TokenType.IN_BITWISE_OR : TokenType.BITWISE_OR;
					case '^' -> next('=') ? TokenType.IN_BITWISE_XOR : TokenType.BITWISE_XOR;
					case '~' -> TokenType.BITWISE_NOT;
					case '?' -> question();
					default -> TokenType.ERROR;
				};
				token = makeToken(type);
			}

			tokens.add(token);
		}
		tokens.add(makeToken(TokenType.EOF));

		return tokens;
	}

	private TokenType question() {
		if(next('.')) {
			return TokenType.QUESTION_DOT;
		}
		else if(next('[')) {
			return TokenType.QUESTION_LSQBR;
		}
		else if(next('?')) {
			return TokenType.QUESTION_QUESTION;
		}

		return TokenType.QUESTION;
	}

	private TokenType lexGreaterThan() {
		if(next('=')) {
			return TokenType.GREATER_EQ;
		}
		else if(next('>')) {
			if(next('=')) {
				return TokenType.IN_BITWISE_SHR;
			}
			else if(next('>')) {
				if(next('=')) {
					return TokenType.IN_BITWISE_USHR;
				}
				return TokenType.BITWISE_USHR;
			}
			return TokenType.BITWISE_SHR;
		}
		return TokenType.GREATER;
	}

	private Token string() {
		advance();

		boolean escape = false;
		while(!isAtEnd()) {
			if(current == '"' && !escape) break;

			if(current == '\n') line++;
			if(escape) escape = false;
			if(current == '\\') escape = true;

			advance();
		}

		return makeToken(TokenType.STRING);
	}

	private Token character() {
		advance();

		boolean escape = false;
		while(!isAtEnd()) {
			if(current == '\'' && !escape) break;

			if(current == '\n') line++;
			if(escape) escape = false;
			if(current == '\\') escape = true;

			advance();
		}

		return makeToken(TokenType.CHAR_LITERAL);
	}

	private Token identifier() {
		while(!isAtEnd() && isValidIdentifierPart(current)) {
			advance();
		}
		if(!isAtEnd()) index--;
		String val = text.substring(start, index);

		return makeToken(switch (val) {
			case "using" -> TokenType.USING;
			case "impl" -> TokenType.IMPL;
			case "interface" -> TokenType.INTERFACE;
			case "match" -> TokenType.MATCH;
			case "case" -> TokenType.CASE;
			case "default" -> TokenType.DEFAULT;
			case "final" -> TokenType.FINAL;
			case "unittest" -> TokenType.UNITTEST;
			case "getter" -> TokenType.GETTER;
			case "setter" -> TokenType.SETTER;
			case "override" -> TokenType.OVERRIDE;
			case "package" -> TokenType.PACKAGE;
			case "def" -> TokenType.FUNCTION;
			case "var" -> TokenType.VAR;
			case "const" -> TokenType.CONST;
			case "class" -> TokenType.CLASS;
			case "enum" -> TokenType.ENUM;
			case "to" -> TokenType.TO;
			case "return" -> TokenType.RETURN;
			case "continue" -> TokenType.CONTINUE;
			case "break" -> TokenType.BREAK;
			case "while" -> TokenType.WHILE;
			case "for" -> TokenType.FOR;
			case "ext" -> TokenType.EXT;
			case "if" -> TokenType.IF;
			case "else" -> TokenType.ELSE;
			case "new" -> TokenType.NEW;
			case "true" -> TokenType.TRUE;
			case "false" -> TokenType.FALSE;
			case "null" -> TokenType.NULL;
			case "this" -> TokenType.THIS;
			case "super" -> TokenType.SUPER;
			case "void" -> TokenType.VOID;
			case "int" -> TokenType.INT;
			case "double" -> TokenType.DOUBLE;
			case "float" -> TokenType.FLOAT;
			case "boolean" -> TokenType.BOOLEAN;
			case "char" -> TokenType.CHAR;
			case "long" -> TokenType.LONG;
			case "byte" -> TokenType.BYTE;
			case "short" -> TokenType.SHORT;
			case "pub" -> TokenType.PUBLIC;
			case "priv" -> TokenType.PRIVATE;
			case "static" -> TokenType.STATIC;
			case "throw" -> TokenType.THROW;
			case "throws" -> TokenType.THROWS;
			case "try" -> TokenType.TRY;
			case "catch" -> TokenType.CATCH;
			case "finally" -> TokenType.FINALLY;
			case "is" -> TokenType.IS;
			default -> TokenType.IDENTIFIER;
		});
	}

	private Token number() {
		while (!isAtEnd() && isNumeric(current)) {
			advance();
		}
		if(!isAtEnd()) index--;
		if(current == 'l' || current == 'L') {
			advance();
			return makeToken(TokenType.NUMBER);
		}
		if(match('.')) {
            do {
                advance();
            } while (!isAtEnd() && isNumeric(current));
			if(!isAtEnd()) index--;
		}
		if(current == 'f' || current == 'F') advance();
		return makeToken(TokenType.NUMBER);
	}

	private boolean match(char c) {
		return current == c && (advance() != -1);
	}

	private boolean next(char c) {
		return text.charAt(index) == c && advance() != -1;
	}

	private boolean isAtEnd() {
		return index == text.length();
	}

	private char advance() {
		current = text.charAt(index);
		column++;
		return text.charAt(index++);
	}

	private Token makeToken(TokenType type) {
		return new Token(type, text.substring(start, index), line, column);
	}

	private static boolean isNumeric(char c) {
		return '0' <= c && c <= '9';
	}

	private static boolean isAlpha(char c) {
		return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
	}

	private static boolean isValidIdentifierStart(char c) {
		return isAlpha(c) || c == '_';
	}

	private static boolean isValidIdentifierPart(char c) {
		return isAlpha(c) || isNumeric(c) || c == '_';
	}

	private static boolean isWhitespace(char c) {
		return c == ' ' || c == '\r' || c == '\t' || c == '\n';
	}

}
