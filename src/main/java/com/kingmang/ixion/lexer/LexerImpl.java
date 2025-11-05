package com.kingmang.ixion.lexer;

import java.io.*;

public class LexerImpl implements Lexer{
    private final StringBuilder sb = new StringBuilder();
    private final PushbackReader reader;
    public int line = 1;
    public int col = 1;

    public LexerImpl(File file) throws FileNotFoundException {
        this.reader = new PushbackReader(new FileReader(file), 5);
    }

    @Override
    public Token tokenize() {
        skipWhitespace();

        int startLine = line;
        int startCol = col;
        char currentChar = peek();

        if (currentChar == '\0') {
            return new Token(TokenType.EOF, startLine, startCol, null);
        }

        if (isAlpha(currentChar)) {
            return consumeIdentifierToken(startLine, startCol);
        }

        if (isDigit(currentChar)) {
            return consumeNumberToken(startLine, startCol);
        }

        if (currentChar == '"') {
            return consumeStringToken(startLine, startCol);
        }

        if (currentChar == '\'') {
            return consumeCharToken(startLine, startCol);
        }

        return consumeOperatorToken(startLine, startCol);
    }


    void skipWhitespace() {
        while (true) {
            char currentChar = peek();

            switch (currentChar) {
                case ' ':
                case '\r':
                case '\t':
                case '\n':
                    advance();
                    break;

                case '/':
                    handleComment();
                    break;

                default:
                    return;
            }
        }
    }

    private void handleComment() {
        char nextChar = peekNext();

        if (nextChar == '/') {
            skipSingleLineComment();
        } else if (nextChar == '*') {
            skipMultiLineComment();
        }
    }


    private void skipSingleLineComment() {
        advance();
        advance();

        while (peek() != '\n' && peek() != '\0') {
            advance();
        }
    }

    private void skipMultiLineComment() {
        advance();
        advance();

        String nextTwoChars;
        do {
            advance();
            nextTwoChars = String.valueOf(peek()) + peekNext();
        } while (!nextTwoChars.equals("*/"));

        advance();
        advance();
    }

    private Token consumeCharToken(int line, int col) {
        advance();

        char currentChar = peek();
        char charValue;

        if (currentChar == '\\') {
            advance();
            char escapeChar = peek();
            charValue = switch (escapeChar) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case '\'' -> '\'';
                case '\\' -> '\\';
                case '0' -> '\0';
                default -> {
                    advance();
                    yield '\\';
                }
            };
            advance();
        } else {
            charValue = currentChar;
            advance();
        }

        if (peek() != '\'') {
            return new Token(TokenType.ERROR, line, col, "Unterminated character literal");
        }

        advance();

        return new Token(TokenType.CHAR, line, col, String.valueOf(charValue));
    }

    private char advance() {
        try {
            int charCode = reader.read();
            if (charCode == -1) return '\0';

            if (charCode == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }

            return (char) charCode;
        } catch (IOException e) {
            e.printStackTrace();
            return '\0';
        }
    }

    private String clearStringBuilder() {
        String result = sb.toString();
        sb.setLength(0);
        return result;
    }

    private Token consumeIdentifierToken(int line, int col) {
        String identifier = consumeIdentifier();
        TokenType tokenType = TokenType.IDENTIFIER;

        TokenType keywordType = TokenType.find(identifier);
        if (keywordType != null) {
            tokenType = keywordType;
        }

        return new Token(tokenType, line, col, identifier);
    }

    private String consumeIdentifier() {
        while (isAlphaNumeric(peek())) {
            sb.append(advance());
        }
        return clearStringBuilder();
    }


    private Token consumeNumberToken(int line, int col) {
        TokenType type = TokenType.INT;

        consumeIntegerPart();

        if (peek() == '.') {
            type = TokenType.FLOAT;
            consumeDecimalPart();
        }

        consumeFloatSuffix(type);
        consumeExponentPart();
        consumeDoubleSuffix();

        String value = clearStringBuilder();
        return new Token(type, line, col, value);
    }


    private void consumeIntegerPart() {
        while (isDigit(peek())) {
            sb.append(advance());
        }
    }


    private void consumeDecimalPart() {
        sb.append(advance()); // consume '.'

        while (isDigit(peek())) {
            sb.append(advance());
        }
    }


    private void consumeFloatSuffix(TokenType type) {
        if (peek() == 'f') {
            type = TokenType.FLOAT;
            sb.append(advance());
        }
    }


    private void consumeExponentPart() {
        char currentChar = peek();
        if (currentChar == 'e' || currentChar == 'E') {
            sb.append(advance());

            currentChar = peek();
            if (currentChar == '-' || currentChar == '+') {
                sb.append(advance());
            }

            while (isDigit(peek())) {
                sb.append(advance());
            }
        }
    }


    private void consumeDoubleSuffix() {
        if (peek() == 'd') {
            sb.append(advance());
        }
    }

    private Token consumeStringToken(int line, int col) {
        advance();

        char currentChar = peek();
        while (currentChar != '"' && currentChar != '\0') {
            sb.append(advance());
            currentChar = peek();
        }

        advance();

        String stringLiteral = clearStringBuilder();
        String escapedString = stringLiteral.translateEscapes();

        return new Token(TokenType.STRING, line, col, escapedString);
    }

    private Token consumeOperatorToken(int line, int col) {
        char currentChar = peek();
        char nextChar = peekNext();
        String twoCharOperator = String.valueOf(new char[]{currentChar, nextChar});

        TokenType longToken = TokenType.find(twoCharOperator);
        TokenType shortToken = TokenType.find(String.valueOf(currentChar));

        if (longToken != null) {
            advance();
            advance();
            return new Token(longToken, line, col, twoCharOperator);
        }

        if (shortToken != null) {
            advance();
            return new Token(shortToken, line, col, String.valueOf(currentChar));
        }

        advance();
        advance();
        return new Token(TokenType.ERROR, line, col, twoCharOperator);
    }

    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }


    private char peek() {
        try {
            int charCode = reader.read();
            if (charCode == -1) return '\0';

            reader.unread(charCode);
            return (char) charCode;
        } catch (IOException e) {
            e.printStackTrace();
            return '\0';
        }
    }

    private char peekNext() {
        try {
            int firstChar = reader.read();
            int secondChar = reader.read();

            if (secondChar == -1) return '\0';

            reader.unread(secondChar);
            reader.unread(firstChar);

            return (char) secondChar;
        } catch (IOException e) {
            e.printStackTrace();
            return '\0';
        }
    }

}