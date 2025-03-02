package com.kingmang.ixion.parser.impl;


import com.kingmang.ixion.exceptions.ParserException;
import com.kingmang.ixion.parser.Node;
import com.kingmang.ixion.parser.Parser;

import com.kingmang.ixion.parser.tokens.Token;
import com.kingmang.ixion.parser.tokens.TokenType;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/*
Синтаксическое дерево, которое строится парсером не является абстрактным,
поскольку узлы кодируют способ их компиляции (но термин все еще применим).
*/

public class ParserImpl implements Parser {

	private int index;
	private final List<Token> tokens;
	private boolean isParsingClass;

	public ParserImpl(List<Token> tokens){
		this.tokens = tokens;
		this.index = 0;
		this.isParsingClass = false;
	}

	@Override
	public Node parse() throws ParserException {
		return program();
	}

	private Node program() throws ParserException {
		ArrayList<Node> declarations = new ArrayList<>();
		ArrayList<Node> imports = new ArrayList<>();

		Node packageName = null;

		if(match(TokenType.PACKAGE)) {
			packageName = packageStatement();
		}

		while(!isAtEnd() && match(TokenType.USING)) {
			imports.add(usingStatement());
		}

		while(!isAtEnd()) {
			declarations.add(declaration());
		}
		return new ProgramNode(packageName, imports, declarations);
	}

	private Node packageStatement() throws ParserException {
		Token packageToken = tokens.get(index - 1);

		Node name = classType();

		consume(TokenType.SEMI, "Expected ';' after package");

		return new PackageNode(packageToken, (TypeNode) name);
	}

	private Node usingStatement() throws ParserException {
		Token importTok = tokens.get(index - 1);
		Node type = classType();

		Token to = null;
		if(match(TokenType.COLON)) {
			to = consume(TokenType.IDENTIFIER, "Expected import alias");
		}

		consume(TokenType.SEMI, "Expected ';' after import");

		return new UsingNode(importTok, type, to);
	}


	private Node declaration() throws ParserException {
		Token accessModifier = null;
		Token staticModifier = null;
		boolean isFinal = false;
		List<Token> functionModifiers = new ArrayList<>();

		while (true) {
			if (match(TokenType.OVERRIDE) && tokens.get(index-1) == null) {
				functionModifiers.add(tokens.get(index - 1));
			} else if ((match(TokenType.PUBLIC) || match(TokenType.PRIVATE)) && tokens.get(index-1) == null) {
				accessModifier = tokens.get(index - 1);
			} else if (match(TokenType.FINAL)) {
				isFinal = true;
			} else if (isParsingClass && match(TokenType.STATIC)) {
				staticModifier = tokens.get(index - 1);
			} else {
				break;
			}
		}

		Token tok = advance();

		return switch(tok.type()) {
			case INTERFACE -> interfaceDeclaration(accessModifier,staticModifier);
			case DEFINE_FUNCTION -> functionDeclaration(accessModifier, staticModifier, functionModifiers);
			case CLASS -> classDeclaration(accessModifier, staticModifier, isFinal);
			case ENUM -> enumDeclaration(accessModifier, staticModifier);
			case THIS -> constructorDeclaration(accessModifier, staticModifier);
			case VAR, CONST -> variableDeclaration(accessModifier, staticModifier);
			//case AT -> annotationDeclaration();
			default -> throw new ParserException(tok, "Expected declaration");
		};

	}


	private Node functionDeclaration(Token access, Token staticModifier, List<Token> annotations) throws ParserException {
		Token name = consume(TokenType.IDENTIFIER, "Expected function name");

		List<Pair<Token, Node>> parameters = typedParameters("function parameter list");

		Node returnType = null;

		if(match(TokenType.COLON)) {
			if(match(TokenType.VOID)) returnType = new TypeNode(tokens.get(index - 1));
			else returnType = type();
		}

		List<Node> throwsList = null;
		if(match(TokenType.THROWS)) {
			throwsList = new ArrayList<>();
			do {
				throwsList.add(basicType());
			} while(match(TokenType.COMMA));
		}

		Node body;
		FunctionDeclarationNode.DeclarationType type = FunctionDeclarationNode.DeclarationType.STANDARD;

		if(match(TokenType.LAMBDA)) {
			body = expression();
			consume(TokenType.SEMI, "Expected ';' after expression");
			type = FunctionDeclarationNode.DeclarationType.EXPRESSION;
		}
		else {
			consume(TokenType.LBRACE, "Expected '{' before function body");

			body = blockStatement();
		}

		return new FunctionDeclarationNode(type, name, body, parameters, returnType, throwsList, access, staticModifier, annotations);
	}

	/*
	private Node unitTestDeclaration() throws ParserException {
		Token unittestToken = tokens.get(index - 1);

		consume(TokenType.LBRACE, "Expected '{' before unittest body");

		Node body = blockStatement();


		return new UnitTestNode(unittestToken, body);
	}

	 */

	private Node constructorDeclaration(Token access, Token staticModifier) throws ParserException {
		if(!isParsingClass) throw new ParserException(tokens.get(index - 1), "Cannot declare constructor outside of class");
		if(staticModifier != null) throw new ParserException(staticModifier, "Cannot declare constructor as static");

		Token constructor = tokens.get(index - 1);

		List<Pair<Token, Node>> parameters = typedParameters("constructor parameter list");

		List<Node> superArgs = null;
		if(match(TokenType.COLON)) {
			superArgs = arguments("super arguments");
		}

		consume(TokenType.LBRACE, "Expected '{' before constructor body");

		Node body = blockStatement();

		return new ThisMethodDeclarationNode(access, constructor, superArgs, parameters, body);
	}


	private Node classDeclaration(Token access, Token staticModifier, boolean isModule) throws ParserException {
		if(staticModifier != null) throw new ParserException(staticModifier, "Cannot mark a class as static");
		Token name = consume(TokenType.IDENTIFIER, "Expected class name");

		Node superclass = null;
		Node interfaze = null;

		if(match(TokenType.EXT)) {
			superclass = basicType();
		}
		if(match(TokenType.IMPL)){
			interfaze = basicType();
		}
		consume(TokenType.LBRACE, "Expected '{' before class body");

		LinkedList<Node> declarations = new LinkedList<>();

		boolean wasParsingClass = isParsingClass;
		isParsingClass = true;
		while(!isAtEnd() && tokens.get(index).type() != TokenType.RBRACE) {
			declarations.add(declaration());
		}
		isParsingClass = wasParsingClass;

		consume(TokenType.RBRACE, "Expected '}' after class body");

		return new ClassDeclarationNode(name, superclass, interfaze, declarations, access, isModule);
	}


	private Node interfaceDeclaration(Token access, Token staticModifier) throws ParserException {
		if(staticModifier != null) throw new ParserException(staticModifier, "Cannot mark an interface as static");
		Token name = consume(TokenType.IDENTIFIER, "Expected interface name");
		List<Node> methods = new ArrayList<>();
		consume(TokenType.LBRACE, "Expected '{' before interface body");
		do{
			match(TokenType.DEFINE_FUNCTION);
			methods.add(functionDeclaration(null, null, null));
		}while (!isAtEnd() && match(TokenType.COMMA));
		consume(TokenType.RBRACE, "Expected '}' after interface body");

		return new InterfaceDeclarationNode(name, methods, access);
	}


	private Node enumDeclaration(Token access, Token staticModifier) throws ParserException {
		if(staticModifier != null) throw new ParserException(staticModifier, "Cannot mark an enum as static");
		Token name = consume(TokenType.IDENTIFIER, "Expected enum name");

		consume(TokenType.LBRACE, "Expected '{' before enum body");

		ArrayList<Token> fields = new ArrayList<>();

		if(tokens.get(index).type() != TokenType.RBRACE) {
			do {
				fields.add(consume(TokenType.IDENTIFIER, "Expected enum field name"));
			} while (!isAtEnd() && match(TokenType.COMMA));
		}

		consume(TokenType.RBRACE, "Expected '}' after enum body");

		return new EnumDeclarationNode(name, fields, access);
	}

	private Node variableDeclaration(Token access, Token staticModifier) throws ParserException {
		boolean isConst = tokens.get(index - 1).type() == TokenType.CONST;
		Token getterToken = null;
		Token setterToken = null;

		if (match(TokenType.LPAREN)) {
			while (true) {
				if (match(TokenType.GETTER)) {
					getterToken = tokens.get(index - 1);
				} else if (match(TokenType.SETTER)) {
					setterToken = tokens.get(index - 1);
				} else {
					break;
				}

				if (!match(TokenType.COMMA)) {
					break;
				}
			}
			consume(TokenType.RPAREN, "Expected ')' after params");
		}


		Token name = consume(TokenType.IDENTIFIER, "Expected variable name");

		Node type = null;
		Node value = null;
		if(match(TokenType.COLON)) {
			type = type();
			if(match(TokenType.EQUALS)) {
				value = expression();
			}
		}
		else {
			consume(TokenType.EQUALS, "Expected '=' after variable name");

			value = expression();
		}

		consume(TokenType.SEMI, "Expected ';' after variable assignment");

		return new VariableDeclarationNode(name, type, value, isConst, access, staticModifier, getterToken, setterToken);
	}

	private Node blockStatement() throws ParserException {
		ArrayList<Node> nodes = new ArrayList<>();
		if(tokens.get(index).type() != TokenType.RBRACE) {
			do {
				if(match(TokenType.VAR) || match(TokenType.CONST)) nodes.add(variableDeclaration(null, null));
				else nodes.add(statement());
			} while (!isAtEnd() && tokens.get(index).type() != TokenType.RBRACE);
		}

		consume(TokenType.RBRACE, "Expected '}' after block");
		return new BlockNode(nodes);
	}

	private Node expressionStatement() throws ParserException {
		Node expr = expression();
		consume(TokenType.SEMI, "Expected ';' after expression");
		return new ExpressionStatementNode(expr);
	}

	private Node returnStatement() throws ParserException {
		Token returnTok = consume(TokenType.RETURN, "Expected 'return'");
		Node expression = null;
		if(tokens.get(index).type() != TokenType.SEMI) {
			expression = expression();
		}
		consume(TokenType.SEMI, "Expected ';' after return");

		return new ReturnNode(returnTok, expression);
	}

	

	private Node ifStatement() throws ParserException {
		Token ifTok = consume(TokenType.IF, "Expected 'if'");

		consume(TokenType.LPAREN, "Expected '(' after if");

		Node condition = expression();

		consume(TokenType.RPAREN, "Expected ')' after condition");

		Node body = statement();

		Node elseBody = null;

		if(match(TokenType.ELSE)) {
			elseBody = statement();
		}

		return new IfStatementNode(ifTok, condition, body, elseBody);
	}

	private Node whileStatement() throws ParserException {
		Token whileTok = consume(TokenType.WHILE, "Expected 'while'");

		consume(TokenType.LPAREN, "Expected '(' after while");

		Node condition = expression();

		consume(TokenType.RPAREN, "Expected ')' after condition");

		Node body = statement();

		return new WhileStatementNode(whileTok, condition, body);
	}


	private Node loopControlStatement() throws ParserException {
		switch (tokens.get(index).type()) {
            case BREAK, CONTINUE -> {
				Token tk = advance();
				consume(TokenType.SEMI, "Expected ';' after break or continue");
				return new LoopControlNode(tk);
			}
            default -> throw new ParserException(tokens.get(index), "Expected 'break' or 'continue'");
		}
	}

	private Node forStatement() throws ParserException {
		Token forTok = consume(TokenType.FOR, "Expected 'for'");

		consume(TokenType.LPAREN, "Expected '(' after for");

		Node init;

		if(match(TokenType.VAR) || match(TokenType.CONST)) {
			init = variableDeclaration(null, null);
		}
		else {
			init = expressionStatement();
		}

		Node condition = expression();

		consume(TokenType.SEMI, "Expected ';' after condition");

		Node iterate = expression();

		consume(TokenType.RPAREN, "Expected ')' before for body");

		Node body = statement();

		return new ForStatementNode(forTok, init, condition, iterate, body);
	}

	private Node throwStatement() throws ParserException {
		Token throwTok = consume(TokenType.THROW, "Expected 'throw'");

		Node throwee = expression();

		consume(TokenType.SEMI, "Expected ';' after throw target");

		return new ThrowNode(throwTok, throwee);
	}


	private Node matchStatement() throws ParserException{
		return null;
	}

	private Node tryStatement() throws ParserException {
		Token tryTok = consume(TokenType.TRY, "Expected 'try'");

		consume(TokenType.LBRACE, "Expected '{' after try");

		Node tryBody = blockStatement();

		ArrayList<CatchNode> catchBlocks = new ArrayList<>();
		while(match(TokenType.CATCH)) {
			consume(TokenType.LPAREN, "Expected '(' after catch");

			Token bindingName = consume(TokenType.IDENTIFIER, "Expected catch exception binding name");

			consume(TokenType.COLON, "Expected ':' between name and type");

			Node exceptionType = basicType();

			consume(TokenType.RPAREN, "Expected ')' after catch clause");

			consume(TokenType.LBRACE, "Expected '{' after catch clause");

			Node catchBody = blockStatement();

			catchBlocks.add(new CatchNode(bindingName, exceptionType, catchBody));
		}

		Node finallyBlock = null;
		if(match(TokenType.FINALLY)) {
			consume(TokenType.LBRACE, "Expected '{' after catch clause");
			finallyBlock = blockStatement();
		}

		if(catchBlocks.isEmpty() && finallyBlock == null) {
			throw new ParserException(tryTok, "'try' block must have at least one catch/finally block");
		}

		return new TryNode(tryBody, catchBlocks, finallyBlock);
	}



	private Node statement() throws ParserException {
		return switch (tokens.get(index).type()) {
			case LBRACE -> { advance(); yield blockStatement(); }
			case IF -> ifStatement();
			case MATCH -> matchStatement();
			case WHILE -> whileStatement();
			case FOR -> forStatement();
			case BREAK, CONTINUE -> loopControlStatement();
            case RETURN -> returnStatement();
			case THROW -> throwStatement();
			case TRY -> tryStatement();
			default -> expressionStatement();
		};
	}

	private Node expression() throws ParserException {
		return assignExpr();
	}

	private Node assignExpr() throws ParserException {
		Node left = ternaryExpr();

		while(matchAssignment()) {
			Token op = tokens.get(index - 1);

			Node right = logicalNullExpr();

			left = new AssignmentNode(left, op, right);
		}

		return left;
	}

	private Node ternaryExpr() throws ParserException {
		Node left = logicalAndExpr();
		if(match(TokenType.QUESTION)) {

			Node trueExpr = expression();

			consume(TokenType.COLON, "Expected ':' after true expression");

			Node falseExpr = expression();

			return new TernaryExprNode(left, trueExpr, falseExpr);
		}
		return left;
	}

	private Node logicalNullExpr() throws ParserException {
		Node left = logicalOrExpr();

		while(match(TokenType.QUESTION_QUESTION)) {
			Token op = tokens.get(index - 1);

			Node right = logicalOrExpr();

			left = new LogicalNullOperatorNode(left, op, right);
		}
		return left;
	}

	private Node logicalOrExpr() throws ParserException {
		Node left = logicalAndExpr();

		while(match(TokenType.LOGICAL_OR)) {
			Token op = tokens.get(index - 1);

			Node right = logicalAndExpr();

			left = new LogicalOperationNode(left, op, right);
		}
		return left;
	}

	private Node logicalAndExpr() throws ParserException {
		Node left = bitwiseOrExpr();

		while(match(TokenType.LOGICAL_AND)) {
			Token op = tokens.get(index - 1);

			Node right = bitwiseOrExpr();

			left = new LogicalOperationNode(left, op, right);
		}
		return left;
	}

	private Node bitwiseOrExpr() throws ParserException {
		Node left = bitwiseXorExpr();

		while(match(TokenType.BITWISE_OR)) {
			Token op = tokens.get(index - 1);

			Node right = bitwiseXorExpr();

			left = new IntegerOperationNode(left, op, right);
		}
		return left;
	}

	private Node bitwiseXorExpr() throws ParserException {
		Node left = bitwiseAndExpr();

		while(match(TokenType.BITWISE_XOR)) {
			Token op = tokens.get(index - 1);

			Node right = bitwiseAndExpr();

			left = new IntegerOperationNode(left, op, right);
		}
		return left;
	}

	private Node bitwiseAndExpr() throws ParserException {
		Node left = equalityExpr();

		while(match(TokenType.BITWISE_AND)) {
			Token op = tokens.get(index - 1);

			Node right = equalityExpr();

			left = new IntegerOperationNode(left, op, right);
		}
		return left;
	}

	private Node equalityExpr() throws ParserException {
		Node left = relativeExpr();

		while(match(TokenType.EQEQ) || match(TokenType.EXEQ) || match(TokenType.TRI_EQ) || match(TokenType.TRI_EXEQ)) {
			Token op = tokens.get(index - 1);

			Node right = relativeExpr();

			left = new EqualityOperationNode(left, op, right);
		}
		return left;
	}

	private Node relativeExpr() throws ParserException {
		Node left = shiftExpr();

		while(match(TokenType.LESS) || match(TokenType.LESS_EQ) || match(TokenType.GREATER) || match(TokenType.GREATER_EQ)
			|| match(TokenType.IS)) {
			Token op = tokens.get(index - 1);

			if(op.type() == TokenType.IS) {
				Node right = type();

				left = new IsNode(left, op, right);
			}
			else {
				Node right = shiftExpr();

				left = new RelativeOperationNode(left, op, right);
			}
		}
		return left;
	}

	private Node shiftExpr() throws ParserException {
		Node left = arithExpr();

		while(match(TokenType.BITWISE_SHL) || match(TokenType.BITWISE_SHR) || match(TokenType.BITWISE_USHR)) {
			Token op = tokens.get(index - 1);

			Node right = arithExpr();

			left = new IntegerOperationNode(left, op, right);
		}
		return left;
	}

	private Node arithExpr() throws ParserException {
		Node left = term();

		while(match(TokenType.PLUS) || match(TokenType.MINUS)) {
			Token op = tokens.get(index - 1);

			Node right = term();

			left = new ArithmeticOperationNode(left, op, right);
		}
		return left;
	}

	private Node term() throws ParserException {
		Node left = cast();

		while(match(TokenType.STAR) || match(TokenType.SLASH) || match(TokenType.PERCENT)) {
			Token op = tokens.get(index - 1);

			Node right = cast();

			left = new ArithmeticOperationNode(left, op, right);
		}
		return left;
	}


	private Node cast() throws ParserException {
		Node left = unary();
		while(match(TokenType.TO)) {
			Token as = tokens.get(index - 1);
			Node type = type();

			left = new ToNode(left, type, as);
		}

		return left;
	}

	private Node unary() throws ParserException {
		if(match(TokenType.INCREMENT) || match(TokenType.DECREMENT)) {
			Token op = tokens.get(index - 1);

			Node right = unary();

			return new UpdateExpressionNode(right, op, true);
		}

		if(match(TokenType.EXCLAIM) || match(TokenType.MINUS) || match(TokenType.BITWISE_NOT)) {
			Token op = tokens.get(index - 1);

			Node right = unary();

			return new UnaryOperationNode(op, right);
		}
		return memberAccess();
	}


	private Node memberAccess() throws ParserException {
		Node left = atom();

		if(match(TokenType.EXCLAIM)) {
			left = new NonNullAssertionNode(left, tokens.get(index - 1));
		}

		while(match(TokenType.DOT) || match(TokenType.QUESTION_DOT) || match(TokenType.LSQBR) || match(TokenType.QUESTION_LSQBR)) {
			if(tokens.get(index - 1).type() == TokenType.LSQBR) {
				Token bracket = tokens.get(index - 1);
				Node index = expression();
				consume(TokenType.RSQBR, "Expected ']' after index");
				left = new IndexAccessNode(bracket, left, index);
				continue;
			}
			else if(tokens.get(index - 1).type() == TokenType.QUESTION_LSQBR) {
				Token bracket = tokens.get(index - 1);
				Node index = expression();
				consume(TokenType.RSQBR, "Expected ']' after index");
				left = new NullableIndexAccessNode(bracket, left, index);
				continue;
			}

			boolean nullable = tokens.get(index - 1).type() == TokenType.QUESTION_DOT;
			Token name = consume(TokenType.IDENTIFIER, "Expected member name");

			if(tokens.get(index).type() == TokenType.LPAREN) {
				List<Node> args = arguments("method arguments");

				left = nullable ? new NullableMethodCallNode(left, name, args, false) : new MethodCallNode(left, name, args, false);
			}
			else {
				left = nullable ? new NullableMemberAccessNode(left, name) : new MemberAccessNode(left, name);
			}
		}

		if(match(TokenType.INCREMENT) || match(TokenType.DECREMENT)) {
			left = new UpdateExpressionNode(left, tokens.get(index - 1), false);
		}

		return left;
	}


	private Node atom() throws ParserException {
		Token tok = advance();
		return switch(tok.type()) {
			case NUMBER -> new NumberNode(tok);
			case STRING -> new StringNode(tok);
			case CHAR_LITERAL -> new CharNode(tok);
			case TRUE, FALSE -> new BooleanNode(tok);
			case NULL -> new NullNode();
			case THIS -> new ThisNode(tok);
			case SUPER -> superCall();
			case NEW -> newObject();
			case LPAREN -> grouping();
			case IDENTIFIER -> variable();
			default -> throw new ParserException(tok, "Expected value");
		};
	}

	private Node newObject() throws ParserException {
		Token newToken = tokens.get(index - 1);
		Node type = basicType();

		if(match(TokenType.LSQBR)) {
			return newArray(newToken, type);
		}

		List<Node> args = arguments("constructor arguments");

		return new ObjectConstructorNode(newToken, type, args);
	}

	private Node newArray(Token newToken, Node type) throws ParserException {
		ArrayList<Node> dimensions = new ArrayList<>();

		do {
			if(match(TokenType.RSQBR)) {
				dimensions.add(null);
			}
			else {
				dimensions.add(expression());
				consume(TokenType.RSQBR, "Expected ']' after array size");
			}
		} while(match(TokenType.LSQBR));

		ArrayConstructorNode.InitValue valueList = null;

		if(match(TokenType.LBRACE)) {
			valueList = arrayInitializeDimension(dimensions.size());
		}

		return new ArrayConstructorNode(newToken, type, dimensions, valueList);
	}

	private ArrayConstructorNode.InitValue arrayInitializeDimension(int dimension) throws ParserException {
		ArrayConstructorNode.InitValue valueList = new ArrayConstructorNode.InitValue(new ArrayList<>());

		if(tokens.get(index).type() != TokenType.RBRACE) {
			do {
				if(dimension == 1) {
					valueList.subValues.add(new ArrayConstructorNode.InitValue(expression()));
				}
				else {
					consume(TokenType.LBRACE, "Expected '{' in multi-dimensional array initializer");
					valueList.subValues.add(arrayInitializeDimension(dimension - 1));
				}
			} while(match(TokenType.COMMA));
		}

		consume(TokenType.RBRACE, "Expected '}' after array initialization");
		return valueList;
	}

	private Node grouping() throws ParserException {
		Node val = expression();
		consume(TokenType.RPAREN, "Expected ')' after expression");
		return new GroupingNode(val);
	}

	private Node variable() throws ParserException {
		Token name = tokens.get(index - 1);
		List<Node> args = new ArrayList<>();
		if (tokens.get(index).type() == TokenType.LPAREN) {
			args = arguments("function arguments");
			return new FunctionCallNode(name, args);
		}

		return new VariableAccessNode(name);
	}

	private Node superCall() throws ParserException {
		Token superTok = tokens.get(index - 1);
		consume(TokenType.DOT, "Expected '.' after super.");

		Token name = consume(TokenType.IDENTIFIER, "Expected super method name");

		List<Node> args = arguments("super arguments");

		return new MethodCallNode(new SuperNode(superTok), name, args, true);
	}


	private Node type() throws ParserException {
		TypeNode type = basicType();

		int dim = 0;
		ArrayList<Integer> nullableDimensions = new ArrayList<>();

		if(match(TokenType.QUESTION)) {
			type.setNullable(true);
		}
		else if(match(TokenType.QUESTION_LSQBR)) {
			type.setNullable(true);
			consume(TokenType.RSQBR, "Expected closing ']'");
			dim++;
		}

		while(match(TokenType.LSQBR) || match(TokenType.QUESTION_LSQBR)) {
			TokenType bracketType = tokens.get(index - 1).type();
			consume(TokenType.RSQBR, "Expected closing ']'");
			if(bracketType == TokenType.QUESTION_LSQBR) nullableDimensions.add(dim);
			dim++;
		}

		if(dim != 0) {
			type = new TypeNode(type, dim, nullableDimensions);
			if(match(TokenType.QUESTION)) {
				type.setNullable(true);
			}
		}

		return type;
	}

	private TypeNode basicType() throws ParserException {
		if(LexerImpl.PRIMITIVE_TYPES.contains(tokens.get(index).type())) {
			return new TypeNode(advance());
		}
		else {
			return classType();
		}
	}

	private TypeNode classType() throws ParserException {
		ArrayList<String> parts = new ArrayList<>();
		Token start = tokens.get(index);
		do {
			Token part = consume(TokenType.IDENTIFIER, "Expected class name");
			parts.add(part.value());
		} while(match(TokenType.DOT));
		return new TypeNode(start, String.join(".", parts));
	}

	private List<Pair<Token, Node>> typedParameters(String name) throws ParserException {
		if (tokens.get(index).type() == TokenType.LPAREN) {
			consume(TokenType.LPAREN, "Expected '(' before " + name);
		} else {
			return new ArrayList<>();
		}

		ArrayList<Pair<Token, Node>> parameters = new ArrayList<>();
		if (tokens.get(index).type() != TokenType.RPAREN) {
			do {
				Token parameterName = consume(TokenType.IDENTIFIER, "Expected parameter name");

				consume(TokenType.COLON, "Expected ':' between parameter name and type");

				Node type = type();

				parameters.add(new Pair<>(parameterName, type));
			} while (match(TokenType.COMMA));
		}

		consume(TokenType.RPAREN, "Expected ')' after " + name);

		return parameters;
	}


	private List<Node> arguments(String name) throws ParserException {
		consume(TokenType.LPAREN, "Expected '(' before " + name);

		ArrayList<Node> args = new ArrayList<>();
		if(tokens.get(index).type() != TokenType.RPAREN) {
			do {
				args.add(expression());
			} while (!isAtEnd() && match(TokenType.COMMA));
		}

		consume(TokenType.RPAREN, "Expected ')' after " + name);

		return args;
	}


	private boolean matchAssignment() {
		switch (tokens.get(index).type()) {
			case EQUALS,
				 IN_PLUS,
				 IN_MINUS,
				 IN_MUL,
				 IN_DIV,
				 IN_MOD,
				 IN_BITWISE_AND,
				 IN_BITWISE_OR,
				 IN_BITWISE_XOR,
				 IN_BITWISE_SHL,
				 IN_BITWISE_SHR,
				 IN_BITWISE_USHR
					-> {
				advance();
				return true;
			}
		}
		return false;
	}

	private boolean match(TokenType type) {
		if(tokens.get(index).type() != type) return false;
		advance();
		return true;
	}

	private Token consume(TokenType type, String message) throws ParserException {
		Token tok = tokens.get(index);
		if(tok.type() != type) throw new ParserException(tok, message);
		return advance();
	}

	private boolean isAtEnd() {
		return tokens.get(index).type() == TokenType.EOF;
	}

	private Token advance() {
		return tokens.get(index++);
	}
}
