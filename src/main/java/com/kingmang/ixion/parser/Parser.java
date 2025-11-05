package com.kingmang.ixion.parser;

import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.PublicAccess;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.lexer.LexerImpl;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.parser.infix.*;
import com.kingmang.ixion.parser.prefix.*;
import org.javatuples.Pair;

import java.util.*;

import static com.kingmang.ixion.lexer.TokenType.*;

/**
 * Parser for the Ixion
 * Converts tokens into an abstract syntax tree (AST)
 */

public class Parser {

    final Map<TokenType, PrefixParselet> prefixParselets = new HashMap<>();
    final Map<TokenType, InfixParselet> infixParselets = new HashMap<>();
    private final LexerImpl tokens;
    private final List<Token> mRead = new ArrayList<>();

    /**
     * Constructor for Parser
     * @param tokens The lexer providing tokens to parse
     */
    public Parser(LexerImpl tokens) {
        this.tokens = tokens;

        // Register infix parsers for operators and accessors
        putInfinix(ASSIGN, new AssignOperatorParser());
        putInfinix(DOT, new PropertyAccessParser());
        putInfinix(LBRACK, new IndexAccessParser());

        // Register prefix parsers for literals and identifiers
        putPrefix(INT, new LiteralParser(false));
        putPrefix(FLOAT, new LiteralParser(false));
        putPrefix(CHAR, new LiteralParser(false));
        putPrefix(DOUBLE, new LiteralParser(false));
        putPrefix(TRUE, new LiteralParser(false));
        putPrefix(FALSE, new LiteralParser(false));
        putPrefix(STRING, new LiteralParser(false));
        putPrefix(LBRACK, new LiteralParser(true));
        putPrefix(IDENTIFIER, new IdentifierParser());

        // Register grouping and function call parsers
        putPrefix(LPAREN, new GroupingParser());
        putInfinix(LPAREN, new CallParser());

        // Register prefix operators
        prefix(ADD, Precedence.PREFIX);
        prefix(SUB, Precedence.PREFIX);
        prefix(MOD, Precedence.PREFIX);
        prefix(NOT, Precedence.PREFIX);

        // Register binary operators with associativity and precedence
        infixLeft(ADD, Precedence.SUM);
        infixLeft(SUB, Precedence.SUM);
        infixLeft(MOD, Precedence.SUM);
        infixLeft(MUL, Precedence.PRODUCT);
        infixLeft(DIV, Precedence.PRODUCT);
        infixRight(POW, Precedence.EXPONENT);

        infixLeft(RANGE, Precedence.COMPARISON);

        // Register comparison operators
        infixLeft(EQUAL, Precedence.COMPARISON);
        infixLeft(NOTEQUAL, Precedence.COMPARISON);
        infixLeft(LT, Precedence.COMPARISON);
        infixLeft(GT, Precedence.COMPARISON);
        infixLeft(LE, Precedence.COMPARISON);
        infixLeft(GE, Precedence.COMPARISON);
        infixLeft(AND, Precedence.AND);
        infixLeft(OR, Precedence.OR);
        infixLeft(XOR, Precedence.XOR);

        // Register postfix operators
        postfix(PLUSPLUS, Precedence.POSTFIX);
        postfix(MINUSMINUS, Precedence.POSTFIX);
    }

    /**
     * Parses the entire program into a list of statements
     * @return List of parsed statements
     */
    public List<Statement> parse() {
        List<Statement> stmts = new ArrayList<>();

        while (notAtEnd()) {
            var stmt = statement();
            if (stmt == null) break;
            stmts.add(stmt);
        }

        return stmts;
    }

    /**
     * Parses an expression with default precedence
     * @return The parsed expression
     */
    public Expression expression() {
        return expression(0);
    }

    /**
     * Parses an expression with given precedence using Pratt parsing
     * @param precedence The minimum precedence to parse
     * @return The parsed expression
     */
    public Expression expression(int precedence) {
        Token token = consume();
        PrefixParselet prefix = prefixParselets.get(token.type());

        if (prefix == null) {
            error(token, "Could not parse.");
            return new BadExpression(getPos(), token);
        }

        Expression left = prefix.parse(this, token);

        while (precedence < getPrecedence()) {
            token = consume();
            InfixParselet infix = infixParselets.get(token.type());
            left = infix.parse(this, left, token);
        }

        return left;
    }

    /**
     * Parses a return statement
     * @return The parsed return statement
     */
    ReturnStatement parseReturn() {
        var pos = getPos();
        Expression expression;

        // Handle return with or without expression on same line
        if (peek().type() != RBRACE && peek().line() == pos.line()) {
            expression = expression();
        } else {
            expression = new EmptyExpression(pos);
        }
        return new ReturnStatement(pos, expression);
    }

    /**
     * Parses a type alias statement (struct, enum, or type alias)
     * @return The parsed type alias statement
     */
    private Statement parseTypeAlias() {
        var pos = getPos();
        Token name = consume(IDENTIFIER, "Expected type name.");
        consume(ASSIGN, "Expected assignment operator.");

        if(match(STRUCT)){
            var generics = new ArrayList<Token>();
            if (match(LBRACK)) {
                var t = consume();
                generics.add(t);
                while (check(COMMA)) {
                    consume();
                    t = consume();
                    generics.add(t);
                }
                consume(RBRACK, "Expected closing bracket after struct generics.");
            }

            consume(LBRACE, "Expected opening curly braces before struct body.");

            List<ParameterStatement> parameters = new ArrayList<>();
            while (!check(RBRACE)) {
                ParameterStatement parameter = parseParameter();
                optional(COMMA);
                parameters.add(parameter);
            }

            consume(RBRACE, "Expected closing curly braces after struct body.");

            return new StructStatement(pos, name, parameters, generics);
        }else if(match(ENUM)){
            consume(LBRACE, "Expected opening curly braces before struct body.");

            List<Token> values = new ArrayList<>();
            while (!check(RBRACE)) {
                Token value = consume(IDENTIFIER, "Expected enum value.");
                optional(COMMA);
                values.add(value);
            }

            consume(RBRACE, "Expected opening curly braces before struct body.");

            return new EnumStatement(pos, name, values);
        }
        var union = parseUnion();

        return new TypeAliasStatement(pos, name, union);
    }

    /**
     * Parses a block statement enclosed in curly braces
     * @return The parsed block statement
     */
    private BlockStatement parseBlock() {
        var pos = getPos();
        List<Statement> statements = new ArrayList<>();
        consume(LBRACE, "Expect '{' before block.");

        while (!check(RBRACE) && notAtEnd()) {
            statements.add(statement());
        }

        consume(RBRACE, "Expect '}' after block.");
        return new BlockStatement(pos, statements, new Context());
    }

    /**
     * Parses an export (public) statement
     * @return The parsed export statement
     */
    private ExportStatement parsePublic() {
        var pos = getPos();
        var stmt = statement();
        if (stmt instanceof PublicAccess) {
            return new ExportStatement(pos, stmt);
        } else {
            System.err.println("Can only export struct, type alias, variable, enum or function.");
            System.exit(65);
            return null;
        }
    }

    /**
     * Parses a function definition
     * @return The parsed function statement
     */
    private DefStatement parseFunction() {
        var pos = getPos();
        Token name = consume(IDENTIFIER, "Expected function name.");

        var generics = new ArrayList<Token>();
        if (match(LBRACK)) {
            var t = consume();
            generics.add(t);
            while (check(COMMA)) {
                consume();
                t = consume();
                generics.add(t);
            }
            consume(RBRACK, "Expected closing bracket after function generics.");
        }

        consume(LPAREN, "Expected opening parentheses after function name.");

        List<ParameterStatement> parameters = new ArrayList<>();
        if (!check(RPAREN)) {
            do {
                parameters.add(parseParameter());
            } while (match(COMMA));
        }

        consume(RPAREN, "Expected closing parentheses after function parameters.");

        TypeStatement returnType = null;
        if (match(COLON)) {
            returnType = parseUnion();
        }

        BlockStatement block = parseBlock();

        DefStatement func =  new DefStatement(pos, name, parameters, returnType, block, generics);
        if (!hasReturnStatement(func.body) && returnType != null) {
            error(name,
                    "Function must return a value of type " + "'" + func.returnType.identifier.source() + "'");
        }
        return func;
    }

    private boolean hasReturnStatement(Statement stmt) {
        if (stmt instanceof BlockStatement block) {
            for (Statement s : block.statements) {
                if (hasReturnStatement(s)) {
                    return true;
                }
            }
            return false;
        }
        else if (stmt instanceof ReturnStatement) {
            return true;
        }
        else if (stmt instanceof IfStatement ifStmt) {
            boolean thenHasReturn = hasReturnStatement(ifStmt.trueBlock);
            boolean elseHasReturn = ifStmt.falseStatement != null &&
                    hasReturnStatement(ifStmt.falseStatement);
            return thenHasReturn && elseHasReturn;
        }
        return false;
    }

    /**
     * Parses a use (import) statement
     * @return The parsed use statement
     */
    private UseStatement parseUse() {
        var pos = getPos();

        if (!match(LT)) {
            error(peek(), "Expected '<' at the start of using path");
        }

        StringBuilder usePath = new StringBuilder();
        boolean first = true;

        while (!check(GT) && notAtEnd()) {
            if (!first) {
                if (!match(DIV)) {
                    error(peek(), "Expected '/' in use path");
                }
                usePath.append("/");
            }

            Token part = consume(IDENTIFIER, "Expected identifier in using path");
            usePath.append(part.source());
            first = false;
        }

        consume(GT, "Expected '>' at the end of import path");

        Optional<Token> id = Optional.empty();
        Token useToken = new Token(STRING, pos.line(), pos.col(), usePath.toString());
        return new UseStatement(pos, useToken, id);
    }

    /**
     * Parses a case (pattern matching) statement
     * @return The parsed case statement
     */
    private Statement parseCase() {
        var pos = getPos();

        var expr = expression();

        consume(LBRACE, "Expected opening curly braces before case body.");
        Map<TypeStatement, Pair<String, BlockStatement>> cases = new HashMap<>();

        while (!check(RBRACE)) {
            var type = parseUnion();
            var s = consume(IDENTIFIER, "Expected name for reified value before `=>` in case statement.");

            consume(ARROW, "Expected `=>` after type before expression in case statement.");
            BlockStatement caseBody;
            if (check(LBRACE)) {
                caseBody = parseBlock();
            } else {
                var stmt = new ExpressionStatement(getPos(), expression());
                caseBody = new BlockStatement(getPos(), List.of(stmt), new Context());
            }
            cases.put(type, new Pair<>(s.source(), caseBody));
        }

        consume(RBRACE, "Expected closing curly braces after case body.");

        return new CaseStatement(pos, expr, cases);
    }

    /**
     * Parses a function or struct parameter
     * @return The parsed parameter statement
     */
    private ParameterStatement parseParameter() {
        var pos = getPos();
        Token name = consume(IDENTIFIER, "Expected field name.");
        consume(COLON, "Expected colon after parameter name.");
        TypeStatement type = parseUnion();
        return new ParameterStatement(pos, name, type);
    }

    private ForStatement parseFor() {
        var pos = getPos();
        Token name = consume(IDENTIFIER, "Need iterator variable name.");
        consume(COLON, "Expected ':' operator.");
        Expression condition = expression();
        BlockStatement block = parseBlock();
        return new ForStatement(pos, name, condition, block);
    }


    /**
     * Parses an if statement with optional else branch
     * @return The parsed if statement
     */
    private IfStatement parseIf() {
        var pos = getPos();
        Expression condition = expression();
        BlockStatement trueBlock = parseBlock();
        Statement falseStatement = null;
        if (match(ELSE)) {
            if (match(IF)) {
                falseStatement = parseIf();
            } else if (check(LBRACE)) {
                falseStatement = parseBlock();
            } else {
                error(peek(), "Invalid end to if-else statement.");
            }
        }
        return new IfStatement(pos, condition, trueBlock, falseStatement);
    }

    /**
     * Parses a while loop statement
     * @return The parsed while statement
     */
    private WhileStatement parseWhile() {
        var pos = getPos();
        Expression condition = expression();
        BlockStatement block = parseBlock();
        return new WhileStatement(pos, condition, block);
    }

    /**
     * Parses any kind of statement based on the current token
     * @return The parsed statement
     */
    private Statement statement() {
        var pos = getPos();
        if (check(ERROR)) {
            throw new Error("error");
        }


        if (match(USE)) return parseUse();
        if (match(PUB)) return parsePublic();
        if (match(TYPEALIAS)) return parseTypeAlias();

        if (match(CASE)) return parseCase();
        if (match(DEF)) return parseFunction();
        if (match(IF)) return parseIf();
        if (check(VARIABLE) || check(CONSTANT)) return parseVariable();
        if (match(FOR)) return parseFor();
        if(match(WHILE)) return parseWhile();
        if (match(RETURN)) return parseReturn();
        if (check(LBRACE)) return parseBlock();
        else return new ExpressionStatement(pos, expression());
    }

    /**
     * Parses a type reference
     * @return The parsed type statement
     */
    private TypeStatement parseType() {
        var pos = getPos();
        Token identifier = consume(IDENTIFIER, "Expected type name.");

        boolean listType = false;
        if (match(LBRACK)) {
            listType = true;
            consume(RBRACK, "Expected ']' in list type.");
        }

        Optional<TypeStatement> t = Optional.empty();
        if (match(DOT)) {
            t = Optional.of(parseType());
        }

        return new TypeStatement(pos, identifier, t, listType);
    }

    /**
     * Parses a union type (multiple types separated by pipes)
     * @return The parsed union type statement
     */
    private TypeStatement parseUnion() {
        var pos = getPos();
        List<TypeStatement> types = new ArrayList<>();
        do {
            if (check(PIPE)) consume();
            var type = parseType();
            types.add(type);
        }
        while (check(TokenType.PIPE));

        TypeStatement t = new UnionTypeStatement(pos, types);
        if (types.size() == 1) {
            t = new TypeStatement(pos, types.getFirst().identifier, Optional.empty(), types.getFirst().listType);
        }

        return t;
    }

    /**
     * Parses a variable declaration
     * @return The parsed variable statement
     */
    private VariableStatement parseVariable() {
        var pos = getPos();
        Token mutability = consume();
        Token name = consume(IDENTIFIER, "Expected variable name.");
        consume(ASSIGN, "Expected assignment operator.");
        Expression expression = expression();
        return new VariableStatement(pos, mutability, name, expression, Optional.empty());
    }


    /**
     * Consumes the current token
     * @return The consumed token
     */
    public Token consume() {
        lookAhead();
        return mRead.removeFirst();
    }

    /**
     * Consumes a token of expected type or reports error
     * @param type The expected token type
     * @param message Error message if type doesn't match
     * @return The consumed token
     */
    public Token consume(TokenType type, String message) {
        if (check(type)) return consume();
        error(peek(), message);
        return null;
    }

    /**
     * Registers a left-associative infix operator
     * @param token The operator token type
     * @param precedence The operator precedence
     */
    public void infixLeft(TokenType token, int precedence) {
        putInfinix(token, new BinaryOperatorParser(precedence, false));
    }

    /**
     * Registers a right-associative infix operator
     * @param token The operator token type
     * @param precedence The operator precedence
     */
    public void infixRight(TokenType token, int precedence) {
        putInfinix(token, new BinaryOperatorParser(precedence, true));
    }

    /**
     * Gets the current parsing position
     * @return The current position
     */
    public Position getPos() {
        return new Position(tokens.line, tokens.col);
    }

    /**
     * Registers a postfix operator
     * @param token The operator token type
     * @param precedence The operator precedence
     */
    public void postfix(TokenType token, int precedence) {
        putInfinix(token, new PostfixOperatorParser(precedence));
    }

    /**
     * Registers a prefix operator
     * @param token The operator token type
     * @param precedence The operator precedence
     */
    public void prefix(TokenType token, int precedence) {
        putPrefix(token, new PrefixOperatorParser(precedence));
    }

    /**
     * Checks if the current token matches the given type
     * @param type The token type to check
     * @return True if current token matches the type
     */
    boolean check(TokenType type) {
        return peek().type() == type;
    }


    /**
     * Reports a parsing error and exits
     * @param line The line number where error occurred
     * @param where Location description
     * @param message Error message
     */
    private static void error(int line, String where, String message) {
        System.err.println("[line " + line + "] Parser error" + where + ": " + message);
        System.exit(1);
    }

    /**
     * Reports a parsing error
     * @param token The token where error occurred
     * @param message The error message
     */
    void error(Token token, String message) {
        if (token.type() == EOF) {
            error(token.line(), " at end", message);
        } else {
            error(token.line(), " at '" + token.source() + "'", message);
        }
    }

    /**
     * Gets the precedence of the next operator
     * @return The precedence value
     */
    int getPrecedence() {
        InfixParselet parser = infixParselets.get(lookAhead().type());
        if (parser != null) return parser.precedence();
        return 0;
    }

    /**
     * Looks ahead at a token at the specified distance without consuming it
     * @param distance The lookahead distance (0 = current token, 1 = next token, etc.)
     * @return The token at the specified distance
     */
    Token lookAhead(int distance) {
        while (mRead.size() <= distance) {
            mRead.add(tokens.tokenize());
        }
        return mRead.get(distance);
    }

    /**
     * Looks ahead at the next token without consuming it
     * @return The next token
     */
    Token lookAhead() {
        while (mRead.isEmpty()) {
            mRead.add(tokens.tokenize());
        }
        return mRead.getFirst();
    }

    /**
     * Checks if there are more tokens to parse
     * @return True if not at end of input
     */
    boolean notAtEnd() {
        return peek().type() != EOF;
    }

    /**
     * Optionally consumes a token if it matches the expected type
     * @param type The token type to optionally consume
     */
    public void optional(TokenType type) {
        if (check(type)) consume();
    }

    /**
     * Peeks at the next token without consuming it
     * @return The next token
     */
    public Token peek() {
        return lookAhead();
    }

    /**
     * Registers an infix parselet
     * @param token The token type
     * @param parselet The infix parselet
     */
    void putInfinix(TokenType token, InfixParselet parselet) {
        infixParselets.put(token, parselet);
    }

    /**
     * Registers a prefix parselet
     * @param token The token type
     * @param parselet The prefix parselet
     */
    void putPrefix(TokenType token, PrefixParselet parselet) {
        prefixParselets.put(token, parselet);
    }

    /**
     * Matches and consumes a token if it matches the expected type
     * @param expected The expected token type
     * @return True if token was matched and consumed
     */
    public boolean match(TokenType expected) {
        Token token = lookAhead();
        if (token.type() != expected) {
            return false;
        }
        consume();
        return true;
    }
}