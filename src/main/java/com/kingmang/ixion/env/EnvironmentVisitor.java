package com.kingmang.ixion.env;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.api.IxionConstant;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.lexer.Token;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.modules.Modules;
import com.kingmang.ixion.runtime.*;
import com.kingmang.ixion.typechecker.TypeUtils;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Visitor for building the symbol table and type environment during compilation
 * Traverses the AST and registers variables, functions, types in appropriate scopes
 */
public class EnvironmentVisitor implements Visitor<Optional<IxType>> {

    public final Context rootContext;
    public final IxFile source;
    public final IxApi ixApi;
    final File file;
    public Context currentContext;

    /**
     * Constructs a new EnvironmentVisitor
     * @param ixApi The API instance for error reporting and system interactions
     * @param rootContext The root context/scope for the compilation unit
     * @param source The source file being processed
     */
    public EnvironmentVisitor(IxApi ixApi, Context rootContext, IxFile source) {
        this.rootContext = rootContext;
        this.source = source;
        this.file = source.file;
        this.currentContext = this.rootContext;
        this.ixApi = ixApi;
    }

    /**
     * Generic visit method that delegates to specific AST node handlers
     * @param stmt The statement to visit
     * @return Optional containing the type if applicable, empty otherwise
     */
    @Override
    public Optional<IxType> visit(Statement stmt) {
        return stmt.accept(this);
    }

    /**
     * Visits a type alias declaration and registers it in the current context
     * @param statement The type alias statement
     * @return Empty optional as type aliases don't produce values
     */
    @Override
    public Optional<IxType> visitTypeAlias(TypeAliasStatement statement) {
        var type = statement.typeStmt.accept(this);
        currentContext.addVariable(statement.identifier.source(), type.orElseThrow());
        return Optional.empty();
    }

    /**
     * Visits an assignment expression and checks mutability constraints
     * @param expr The assignment expression
     * @return Empty optional as assignments don't produce types in environment phase
     */
    @NotNull
    @Override
    public Optional<IxType> visitAssignExpr(AssignExpression expr) {
        expr.left.accept(this);
        expr.right.accept(this);

        if (expr.left instanceof IdentifierExpression identifier) {
            var mut = currentContext.getVariableMutability(identifier.identifier.source());
            if (mut == IxionConstant.Mutability.IMMUTABLE) {
                new MutabilityException().send(ixApi, file, identifier, identifier.identifier.source());
            }

        } else if (expr.left instanceof PropertyAccessExpression pa) {
            // Property assignment - handled in type checking phase
        } else {
            new ImplementationException().send(ixApi, file, expr, "Assignment not implemented for any recipient but identifier yet");
        }

        return Optional.empty();
    }

    /**
     * Visits a malformed expression node
     * @param expr The bad expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    /**
     * Visits a binary expression and processes both operands
     * @param expr The binary expression
     * @return Empty optional as binary expressions are type-checked later
     */
    @NotNull
    @Override
    public Optional<IxType> visitBinaryExpr(BinaryExpression expr) {
        expr.left.accept(this);
        expr.right.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a block statement and processes all statements within it
     * Also checks for unreachable code after return statements
     * @param statement The block statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitBlockStmt(BlockStatement statement) {
        boolean returned = false;
        for (var stmt : statement.statements) {
            stmt.accept(this);
            if (!returned) {
                if (stmt instanceof ReturnStatement) returned = true;
            } else {
                new UnreachableException().send(ixApi, file, stmt);
            }
        }
        return Optional.empty();
    }

    /**
     * Visits a function call expression and processes the callee and arguments
     * @param expr The call expression
     * @return Empty optional as function calls are type-checked later
     */
    @NotNull
    @Override
    public Optional<IxType> visitCall(CallExpression expr) {
        expr.item.accept(this);
        for (var arg : expr.arguments) {
            arg.accept(this);
        }
        return Optional.empty();
    }

    /**
     * Visits an empty expression
     * @param empty The empty expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    /**
     * Visits an empty list expression and determines its type
     * @param emptyList The empty list expression
     * @return Optional containing the list type
     */
    @NotNull
    @Override
    public Optional<IxType> visitEmptyList(EmptyListExpression emptyList) {
        var bt = TypeUtils.getFromString(emptyList.tokenType.source());
        var lt = new ListType(bt);
        emptyList.setRealType(lt);
        return Optional.of(lt);
    }

    /**
     * Visits an enum declaration
     * @param statement The enum statement
     * @return Empty optional as enums are processed differently
     */
    @Override
    public Optional<IxType> visitEnum(EnumStatement statement) {
        return Optional.empty();
    }

    /**
     * Visits an export statement and processes the exported declaration
     * @param statement The export statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitExport(ExportStatement statement) {
        statement.stmt.accept(this);
        return Optional.empty();
    }

    /**
     * Visits an expression statement and processes its expression
     * @param statement The expression statement
     * @return The result of visiting the expression
     */
    @Override
    public Optional<IxType> visitExpressionStmt(ExpressionStatement statement) {
        return statement.expression.accept(this);
    }

    /**
     * Visits a for loop statement and sets up the loop variable scope
     * @param statement The for statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitFor(ForStatement statement) {
        var childEnvironment = statement.block.context;
        childEnvironment.parent = currentContext;

        currentContext = childEnvironment;

        statement.expression.accept(this);

        currentContext.addVariable(statement.name.source(), new UnknownType());

        statement.block.accept(this);

        currentContext = currentContext.parent;

        return Optional.empty();
    }

    /**
     * Visits a function declaration and registers it in the current context
     * Sets up the function's parameter scope and processes the function body
     * @param statement The function statement
     * @return Optional containing the function type definition
     */
    @Override
    public Optional<IxType> visitFunctionStmt(DefStatement statement) {
        String name = statement.name.source();
        List<String> generics = statement.generics.stream().map(Token::source).toList();

        var childEnvironment = statement.body.context;
        childEnvironment.parent = currentContext;

        // Annotate parameters in the current scope
        List<Pair<String, IxType>> parameters = new ArrayList<>();
        for (var param : statement.parameters) {
            var pt = param.type.accept(this);
            if (pt.isPresent()) {
                IxType t = pt.get();
                if (t instanceof UnknownType ut && generics.contains(ut.typeName)) {
                    var gt = new GenericType(ut.typeName);
                    parameters.add(Pair.with(param.name.source(), gt));
                    childEnvironment.addVariable(param.name.source(), gt);

                } else {
                    childEnvironment.addVariable(param.name.source(), t);
                    parameters.add(Pair.with(param.name.source(), t));
                }
            } else {
                System.err.println("pt not present");
                System.exit(783);
            }
        }

        var funcType = new DefType(name, parameters, generics);
        if (statement.returnType != null) {
            var ttt = statement.returnType.accept(this);
            funcType.returnType = ttt.get();
        }
        currentContext.addVariableOrError(ixApi, name, funcType, file, statement);

        currentContext = childEnvironment;
        statement.body.accept(this);

        currentContext = currentContext.parent;
        return Optional.of(funcType);
    }

    /**
     * Visits a grouping expression (parentheses) and processes the inner expression
     * @param expr The grouping expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitGroupingExpr(GroupingExpression expr) {
        expr.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Visits an identifier expression and looks up the variable in the current context
     * @param expr The identifier expression
     * @return Optional containing the variable's type if found
     */
    @NotNull
    @Override
    public Optional<IxType> visitIdentifierExpr(IdentifierExpression expr) {
        return Optional.ofNullable(currentContext.getVariable(expr.identifier.source()));
    }

    /**
     * Visits an if statement and sets up the conditional branch scopes
     * @param statement The if statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitIf(IfStatement statement) {
        var childEnvironment = statement.trueBlock.context;
        childEnvironment.parent = currentContext;
        currentContext = childEnvironment;
        statement.condition.accept(this);
        statement.trueBlock.accept(this);
        if (statement.falseStatement != null) statement.falseStatement.accept(this);

        currentContext = currentContext.parent;
        return Optional.empty();
    }

    /**
     * Visits a use/import statement and imports module exports into current context
     * @param statement The use statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitUse(UseStatement statement) {
        String requestedImport = statement.stringLiteral.source();
        if (Modules.modules.containsKey(requestedImport)) {
            var ree = Modules.getExports(requestedImport);
            for (var ft : ree) {
                var typeName = ft.name;
                this.currentContext.addVariableOrError(ixApi, typeName, ft, file, statement);
            }
        }

        return Optional.empty();
    }

    /**
     * Visits an index access expression (array/list indexing)
     * @param expr The index access expression
     * @return Empty optional (handled in type checking phase)
     */
    @NotNull
    @Override
    public Optional<IxType> visitIndexAccess(IndexAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * Visits a literal expression and determines its built-in type
     * @param expr The literal expression
     * @return Optional containing the literal's type
     */
    @NotNull
    @Override
    public Optional<IxType> visitLiteralExpr(LiteralExpression expr) {
        var t = TypeUtils.getFromToken(expr.literal.type());
        if (t == null) {
            new ImplementationException().send(ixApi, file, expr, "This should never happen. All literals should be builtin, for now.");
        } else {
            expr.setRealType(t);
        }
        return Optional.ofNullable(t);
    }

    /**
     * Visits a list literal expression and processes all entries
     * @param expr The list literal expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitLiteralList(LiteralListExpression expr) {
        if (expr.entries.isEmpty()) {
            new ListLiteralIncompleteException().send(ixApi, file, expr);
        }

        for (var entry : expr.entries) {
            entry.accept(this);
        }
        return Optional.empty();
    }

    /**
     * Visits a match statement and sets up case pattern matching scopes
     * @param statement The match statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitMatch(CaseStatement statement) {
        statement.expression.accept(this);
        statement.cases.forEach((keyType, pair) -> {
            String id = pair.getValue0();
            BlockStatement block = pair.getValue1();
            keyType.accept(this);
            // create environment for case

            var pt = keyType.accept(this);
            if (pt.isPresent()) {
                IxType t = pt.get();
                statement.types.put(keyType, t);
            } else {
                System.err.println("pt not present");
                System.exit(783);
            }
            var childEnvironment = block.context;
            childEnvironment.parent = currentContext;
            childEnvironment.addVariable(id, statement.types.get(keyType));
            currentContext = childEnvironment;
            block.accept(this);
            currentContext = currentContext.parent;
        });

        return Optional.empty();
    }

    /**
     * Visits a module access expression (qualified name access)
     * @param expr The module access expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitModuleAccess(ModuleAccessExpression expr) {
        expr.foreign.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a parameter statement (function parameter declaration)
     * @param statement The parameter statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitParameterStmt(ParameterStatement statement) {
        return Optional.empty();
    }

    /**
     * Visits a postfix expression (increment/decrement operators)
     * @param expr The postfix expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitPostfixExpr(PostfixExpression expr) {
        expr.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a prefix expression (unary operators)
     * @param expr The prefix expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitPrefix(PrefixExpression expr) {
        expr.right.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a property access expression (dot notation)
     * @param expr The property access expression
     * @return Empty optional
     */
    @NotNull
    @Override
    public Optional<IxType> visitPropertyAccess(PropertyAccessExpression expr) {
        expr.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a return statement and processes the return expression
     * @param statement The return statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitReturnStmt(ReturnStatement statement) {
        statement.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Visits a struct declaration and registers the struct type in the current context
     * Processes all struct fields and handles generics
     * @param statement The struct statement
     * @return Optional containing the struct type definition
     */
    @Override
    public Optional<IxType> visitStruct(StructStatement statement) {
        var fieldNames = new String[statement.fields.size()];
        var fieldTypes = new IxType[statement.fields.size()];

        List<Pair<String, IxType>> parameters = new ArrayList<>();
        for (int i = 0; i < fieldNames.length; i++) {
            var field = statement.fields.get(i);
            fieldNames[i] = field.name.source();
            if (TokenType.isKeyword(fieldNames[i])) {
                new ReservedWordException().send(ixApi, file, statement.fields.get(i), fieldNames[i]);
            }
            var fieldT = field.type.accept(this);
            if (fieldT.isPresent()) {
                fieldTypes[i] = fieldT.get();
                parameters.add(new Pair<>(fieldNames[i], fieldTypes[i]));
            } else {
                System.err.println("fieldT not present");
                System.exit(429);
            }
        }
        String name = statement.name.source();
        List<String> generics = statement.generics.stream().map(Token::source).collect(Collectors.toList());

        StructType structType = new StructType(name, parameters, generics);
        structType.qualifiedName = source.getFullRelativePath() + "$" + name;
        structType.parentName = source.getFullRelativePath();
        currentContext.addVariableOrError(ixApi, name, structType, file, statement);

        return Optional.of(structType);
    }

    /**
     * Visits a type statement and resolves the type reference
     * Handles both built-in types, custom types, and list types
     * @param statement The type statement
     * @return Optional containing the resolved type
     */
    @Override
    public Optional<IxType> visitTypeAlias(TypeStatement statement) {
        IxType type;
        if (statement.next.isEmpty()) {
            var bt = TypeUtils.getFromString(statement.identifier.source());
            type = Objects.requireNonNullElseGet(bt, () -> new UnknownType(statement.identifier.source()));
            if (statement.listType) {
                type = new ListType(type);
            }
        }
        else {
            StringBuilder path = new StringBuilder(statement.identifier.source());
            var ptr = statement.next;
            while (ptr.isPresent()) {
                path.append(".").append(ptr.get().identifier.source());
                ptr = ptr.get().next;
            }
            type = Objects.requireNonNullElse(
                    currentContext.getVariableTyped(path.toString(), StructType.class),
                    new UnknownType(path.toString())
            );
        }
        return Optional.of(type);
    }

    /**
     * Visits a union type declaration and creates the union type
     * @param statement The union type statement
     * @return Optional containing the union type definition
     */
    @Override
    public Optional<IxType> visitUnionType(UnionTypeStatement statement) {
        var union = new UnionType(statement.types.stream()
                .map(type -> type.accept(this).orElseThrow())
                .collect(Collectors.toSet())
        );
        return Optional.of(union);
    }

    /**
     * Visits a variable declaration and registers it in the current context
     * Handles mutability and type inference from initializer expressions
     * @param statement The variable statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitVariable(VariableStatement statement) {
        var t = statement.expression.accept(this);

        IxType type;
        type = t.orElseGet(UnknownType::new);

        var mut = IxionConstant.Mutability.IMMUTABLE;
        if (statement.mutability.type() == TokenType.VARIABLE) {
            mut = IxionConstant.Mutability.MUTABLE;
        }

        currentContext.addVariableOrError(ixApi, statement.name.source(), type, file, statement);
        currentContext.setVariableMutability(statement.name.source(), mut);
        return Optional.empty();
    }

    /**
     * Visits a while loop statement and sets up the loop scope
     * @param statement The while statement
     * @return Empty optional
     */
    @Override
    public Optional<IxType> visitWhile(WhileStatement statement) {
        var childEnvironment = statement.block.context;
        childEnvironment.parent = currentContext;

        currentContext = childEnvironment;
        statement.condition.accept(this);

        statement.block.accept(this);

        currentContext = currentContext.parent;
        return Optional.empty();
    }
}