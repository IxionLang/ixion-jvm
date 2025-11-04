package com.kingmang.ixion.lexer;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

public enum TokenType {

    CASE("case"),
    WITH("with"),
    FOR("for"),
    WHILE("while"),
    IF("if"),
    ELSE("else"),
    RETURN("return"),
    STRUCT("struct"),
    ENUM("enum"),
    DEF("def"),
    TYPEALIAS("type"),
    CONSTANT("const"),
    VARIABLE("var"),
    PUB("pub"),
    USE("use"),
    NEW("new"),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),
    LBRACK("["),
    RBRACK("]"),
    COMMA(","),
    VARARGS("..."),
    RANGE(".."),
    DOT("."),
    MODULE("::"),
    COLON(":"),
    DEFAULT("_"),

    ASSIGN("="),
    GE(">="),
    LE("<="),
    GT(">"),
    LT("<"),
    EQUAL("=="),
    NOTEQUAL("!="),
    AND("&&"),
    OR("||"),
    XOR("^"),
    ADD("+"),
    MUL("*"),
    SUB("-"),
    DIV("/"),
    MOD("%"),
    POW("**"),
    PIPE("|"),
    ARROW("=>"),
    WALRUS(":="),

    NOT("!"),
    PLUSPLUS("++"),
    MINUSMINUS("--"),

    TRUE("true"),
    FALSE("false"),
    STRING,
    IDENTIFIER,
    INT,
    FLOAT,
    DOUBLE,
    NUMBER,

    ERROR,
    EOF;

    private static final HashMap<String, TokenType> matcher = new HashMap<>();
    private static final Set<TokenType> KEYWORD_TYPES = EnumSet.of(
            CASE, FOR, WHILE, IF, ELSE, RETURN, STRUCT, ENUM, DEF,
            TYPEALIAS, CONSTANT, VARIABLE, PUB, USE, NEW, TRUE, FALSE
    );

    static {
        for (var tokenType : TokenType.values()) {
            if (tokenType.representation != null) matcher.put(tokenType.representation, tokenType);
            if (tokenType.alternate != null) matcher.put(tokenType.alternate, tokenType);
        }
    }

    public final String representation;
    public final String alternate;

    TokenType() {
        this.representation = null;
        this.alternate = null;
    }

    TokenType(String representation) {
        this.representation = representation;
        this.alternate = null;
    }

    TokenType(String representation, String alternate) {
        this.representation = representation;
        this.alternate = alternate;
    }

    public static TokenType find(String representation) {
        return matcher.get(representation);
    }

    public boolean isKeyword() {
        return KEYWORD_TYPES.contains(this);
    }

    public static boolean isKeyword(String str) {
        TokenType tokenType = find(str);
        return tokenType != null && tokenType.isKeyword();
    }
}