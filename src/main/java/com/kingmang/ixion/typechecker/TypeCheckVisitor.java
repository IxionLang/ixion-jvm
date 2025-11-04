package com.kingmang.ixion.typechecker;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.lexer.Position;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.runtime.*;
import org.javatuples.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Visitor for type checking and validation of AST nodes
 * Ensures type safety and resolves type information throughout the program
 */
public class TypeCheckVisitor implements Visitor<Optional<IxType>> {

    public final Context rootContext;
    public final File file;
    public final IxApi ixApi;
    private final Stack<DefType> functionStack = new Stack<>();
    public Context currentContext;

    /**
     * @param ixApi The API instance for error reporting
     * @param rootContext The root context for variable resolution
     * @param ixFile The source file being type checked
     */
    public TypeCheckVisitor(IxApi ixApi, Context rootContext, IxFile ixFile) {
        this.rootContext = rootContext;
        this.file = ixFile.file;
        this.currentContext = this.rootContext;
        this.ixApi = ixApi;
    }

    @Override
    public Optional<IxType> visit(Statement stmt) {
        return stmt.accept(this);
    }

    /**
     * @param statement Type alias statement to process
     * @return Empty optional as type aliases don't produce values
     */
    @Override
    public Optional<IxType> visitTypeAlias(TypeAliasStatement statement) {
        var a = currentContext.getVariable(statement.identifier.source());

        var resolvedTypes = new HashSet<IxType>();
        if (a instanceof UnionType ut) {
            extractedMethodForUnions(resolvedTypes, ut, statement);

            currentContext.setVariableType(statement.identifier(), ut);
        }

        return Optional.empty();
    }

    /**
     * @param expr Assignment expression to type check
     * @return Empty optional as assignments don't produce values
     */
    @Override
    public Optional<IxType> visitAssignExpr(AssignExpression expr) {
        expr.left.accept(this);
        expr.right.accept(this);

        switch (expr.left) {
            case IdentifierExpression id -> {
                if (expr.left.realType != expr.right.realType) {
                    new BadAssignmentException().send(ixApi, file, expr, id.identifier.source(), expr.left.realType.getName(), expr.right.realType.getName());
                }
            }
            case PropertyAccessExpression pa -> {
                var lType = expr.left.realType;
                var rType = expr.right.realType;
                if (!TypeResolver.typesMatch(lType, rType)) {
                    new ParameterTypeMismatchException().send(ixApi, file, expr.left, rType.getName(), lType.getName());
                }

            }
            default -> throw new IllegalStateException("Unexpected value: " + expr.left.realType);
        }

        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    /**
     * @param expr Binary expression to type check
     * @return Optional containing the result type of the binary operation
     */
    @Override
    public Optional<IxType> visitBinaryExpr(BinaryExpression expr) {
        var t1 = expr.left.accept(this);
        var t2 = expr.right.accept(this);

        if (t1.isEmpty() || t2.isEmpty()) {
            new ImplementationException().send(ixApi, file, expr, "Types in binary expression not determined.");
            return Optional.empty();
        }

        if (t1.get() == BuiltInType.ANY || t2.get() == BuiltInType.ANY) {
            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.source(), t1.get(), t2.get());
            return Optional.empty();
        }

        var totalType = t1.get();

        switch (expr.operator.type()) {
            case ADD, SUB, MUL, DIV, MOD -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    totalType = BuiltInType.widen(bt1, bt2);
                } else {
                    new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.source(), t1.get(), t2.get());
                }
            }
            case EQUAL, NOTEQUAL, LT, GT, LE, GE -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    if (expr.operator.type() != TokenType.EQUAL && expr.operator.type() != TokenType.NOTEQUAL) {
                        if (bt1 == BuiltInType.STRING || bt2 == BuiltInType.STRING) {
                            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.source(), bt1.getName(), bt2.getName());
                        } else if (bt1 == BuiltInType.BOOLEAN || bt2 == BuiltInType.BOOLEAN) {
                            new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.source(), bt1.getName(), bt2.getName());

                        }
                    }
                    totalType = BuiltInType.BOOLEAN;
                }
            }
            case AND, OR, XOR -> {
                if (t1.get() instanceof BuiltInType bt1 && t2.get() instanceof BuiltInType bt2) {
                    if (bt1 != BuiltInType.BOOLEAN || bt2 != BuiltInType.BOOLEAN) {
                        new CannotApplyOperatorException().send(ixApi, file, expr, expr.operator.source(), bt1.getName(), bt2.getName());
                    }
                    totalType = BuiltInType.BOOLEAN;
                }
            }

            default -> {
            }
        }

        expr.left.realType = t1.get();
        expr.right.realType = t2.get();

        expr.realType = totalType;

        return Optional.of(totalType);
    }

    /**
     * @param statement Block statement to type check
     * @return Empty optional as blocks don't produce values
     */
    @Override
    public Optional<IxType> visitBlockStmt(BlockStatement statement) {
        for (var stmt : statement.statements) {
            stmt.accept(this);
        }
        return Optional.empty();
    }

    /**
     * @param expr Function call expression to type check
     * @return Optional containing the return type of the function call
     */
    @Override
    public Optional<IxType> visitCall(CallExpression expr) {
        var e = expr.item.accept(this);
        if (e.isEmpty())
            IxApi.exit("Type checking failed to resolve function in ["
                    + expr.pos().line() + ":" + expr.pos().col()
                    + "]", 95);

        var t = e.orElseThrow();
        if (t instanceof StructType st) {
            if (st.parameters.size() != expr.arguments.size()) {
                var params = st.parameters.stream().map(s -> s.getValue1().getName()).collect(Collectors.joining(", "));
                new FunctionSignatureMismatchException().send(ixApi, file, expr.item, st.name, params);
                return Optional.empty();
            }
            updateUnknownParameters(expr, st);

            CollectionUtil.zip(st.parameters, expr.arguments, (param, arg) -> {
                var at = arg.accept(this);
                at.ifPresent(type -> typecheckCallParameters(param, arg, type));
            });
        }

        if (t instanceof DefType ft) {
            var rt = ft.returnType;
            if (ft.hasGenerics()) {

                var specialization = ft.buildSpecialization(expr.arguments);

                ft.specializations.add(specialization);

                if (rt instanceof GenericType(String key)) {
                    rt = specialization.get(key);
                }
            }

            expr.realType = rt;

            return Optional.of(rt);
        } else if (t instanceof StructType structType) {
            expr.realType = structType;
            return Optional.of(structType);
        } else {
            new MethodNotFoundException().send(ixApi, file, expr.item, expr.item);
        }

        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    /**
     * @param emptyList Empty list expression to type check
     * @return Optional containing the type of the empty list
     */
    @Override
    public Optional<IxType> visitEmptyList(EmptyListExpression emptyList) {
        if (emptyList.realType != null) {
            return Optional.of(emptyList.realType);
        }
        if (!functionStack.isEmpty()) {
            var functionType = functionStack.peek();

            if (functionType.returnType instanceof ListType) {
                emptyList.realType = functionType.returnType;
                return Optional.of(functionType.returnType);
            }
        }
        new TypeNotResolvedException().send(ixApi, file, emptyList, "Cannot determine type of empty list");
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitEnum(EnumStatement statement) {
        return Optional.empty();
    }

    /**
     * @param statement Export statement to process
     * @return Empty optional as exports don't produce values
     */
    @Override
    public Optional<IxType> visitExport(ExportStatement statement) {
        statement.stmt.accept(this);
        return Optional.empty();
    }

    /**
     * @param statement Expression statement to type check
     * @return Optional containing the type of the expression
     */
    @Override
    public Optional<IxType> visitExpressionStmt(ExpressionStatement statement) {
        return statement.expression.accept(this);
    }

    /**
     * @param statement For loop statement to type check
     * @return Empty optional as loops don't produce values
     */
    @Override
    public Optional<IxType> visitFor(ForStatement statement) {

        currentContext = statement.block.context;

        var b = statement.expression.accept(this);
        if (b.isPresent()) {
            switch (b.get()) {
                case ExternalType et -> {
                    if (et.foundClass().getName().equals("java.util.Iterator")) {
                        currentContext.setVariableType(statement.name.source(), BuiltInType.INT);
                    }
                }
                case ListType lt -> {
                    currentContext.setVariableType(statement.name.source(), lt.contentType());
                }
                default -> new NotIterableException().send(ixApi, file, statement.expression, b.get().getName());
            }
        }

        statement.block.accept(this);

        currentContext = currentContext.getParent();

        return Optional.empty();

    }

    /**
     * @param statement Function statement to type check
     * @return Empty optional as function definitions don't produce values
     */
    @Override
    public Optional<IxType> visitFunctionStmt(DefStatement statement) {

        var funcType = currentContext.getVariableTyped(statement.name.source(), DefType.class);
        if (funcType != null) {
            functionStack.add(funcType);
            var childEnvironment = statement.body.context;

            var parametersBefore = funcType.parameters;
            var parametersAfter = new ArrayList<Pair<String, IxType>>();

            for (var param : parametersBefore) {
                if (param.getValue1() instanceof UnknownType ut) {
                    var attempt = currentContext.getVariable(ut.typeName);
                    if (attempt != null) {
                        childEnvironment.setVariableType(param.getValue0(), attempt);
                        var nt = param.setAt1(attempt);
                        parametersAfter.add(nt);
                    } else {
                        new IdentifierNotFoundException().send(ixApi, file, statement, ut.typeName);
                        parametersAfter.add(param);
                    }
                } else if (param.getValue1() instanceof UnionType ut) {
                    parametersAfter.add(param);
                    var resolvedTypes = new HashSet<IxType>();
                    extractedMethodForUnions(resolvedTypes, ut, statement);

                    currentContext.setVariableType(param.getValue0(), ut);
                } else {
                    parametersAfter.add(param);

                }
            }
            funcType.parameters.clear();
            funcType.parameters.addAll(parametersAfter);

            if (funcType.returnType instanceof UnknownType ut) {
                var attempt = currentContext.getVariable(ut.typeName);
                if (attempt != null) {
                    funcType.returnType = attempt;
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, statement, ut.typeName);
                }
            }

            currentContext = childEnvironment;

            statement.body.accept(this);

            if (!funcType.hasReturn2) {
                var returnStmt = new ReturnStatement(
                        new Position(0, 0),
                        new EmptyExpression(new Position(0, 0))
                );
                statement.body.statements.add(returnStmt);
            }

            currentContext = currentContext.getParent();
            functionStack.pop();


        }
        return Optional.empty();
    }

    /**
     * @param expr Grouping expression to type check
     * @return Optional containing the type of the grouped expression
     */
    @Override
    public Optional<IxType> visitGroupingExpr(GroupingExpression expr) {
        return expr.expression.accept(this);
    }

    /**
     * @param expr Identifier expression to resolve
     * @return Optional containing the type of the identifier
     */
    @Override
    public Optional<IxType> visitIdentifierExpr(IdentifierExpression expr) {
        var t = currentContext.getVariable(expr.identifier.source());
        if (t != null) {
            if (t instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.typeName);
                if (attempt != null) {
                    t = attempt;
                }
            }
            expr.realType = t;
        } else {
            new IdentifierNotFoundException().send(ixApi, file, expr, expr.identifier.source());
        }
        return Optional.ofNullable(t);
    }

    /**
     * @param statement If statement to type check
     * @return Empty optional as if statements don't produce values
     */
    @Override
    public Optional<IxType> visitIf(IfStatement statement) {
        currentContext = statement.trueBlock.context;
        statement.condition.accept(this);
        statement.trueBlock.accept(this);
        if (statement.falseStatement != null) statement.falseStatement.accept(this);

        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitUse(UseStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitIndexAccess(IndexAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * @param expr Literal expression to type check
     * @return Optional containing the type of the literal
     */
    @Override
    public Optional<IxType> visitLiteralExpr(LiteralExpression expr) {
        var t = expr.realType;
        if (t == null) {
            new ImplementationException().send(ixApi, file, expr, "This should never happen. All literals should be builtin, for now.");
        }
        return Optional.ofNullable(t);
    }

    /**
     * @param expr List literal expression to type check
     * @return Optional containing the type of the list
     */
    @Override
    public Optional<IxType> visitLiteralList(LiteralListExpression expr) {

        var firstType = expr.entries.get(0).accept(this);

        firstType.ifPresent(type -> {
            expr.realType = new ListType(type);

            for (int i = 0; i < expr.entries.size(); i++) {
                var t = expr.entries.get(i).accept(this);
                if (t.isPresent()) {
                    if (!(t.get().equals(type))) {
                        new ListTypeException().send(ixApi, file, expr.entries.get(i), t.get().getName(), type.getName());
                        break;
                    }
                }
            }
        });

        return Optional.of(expr.realType);
    }

    /**
     * @param statement Match statement to type check
     * @return Empty optional as match statements don't produce values
     */
    @Override
    public Optional<IxType> visitMatch(MatchStatement statement) {
        statement.expression.accept(this);

        if (statement.expression.realType instanceof UnionType ut) {
            var typesToCover = new HashSet<>(ut.types);
            statement.cases.forEach((keyTypeStmt, pair) -> {
                String id = pair.getValue0();
                BlockStatement block = pair.getValue1();
                var caseType = statement.types.get(keyTypeStmt);
                if (caseType instanceof UnknownType ukt) {
                    var attempt = currentContext.getVariable(ukt.typeName);
                    if (attempt != null) {
                        caseType = attempt;
                    }
                }

                typesToCover.remove(caseType);

                var childEnvironment = block.context;
                childEnvironment.setParent(currentContext);
                childEnvironment.setVariableType(id, caseType);

                currentContext = childEnvironment;
                block.accept(this);
                currentContext = currentContext.getParent();
            });
            if (!typesToCover.isEmpty()) {
                new MatchCoverageException().send(ixApi, file, statement, ut, CollectionUtil.joinConjunction(typesToCover));
            }


        } else {
            new TypeNotResolvedException().send(ixApi, file, statement.expression, "");
        }

        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitModuleAccess(ModuleAccessExpression expr) {
        expr.foreign.accept(this);
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitParameterStmt(ParameterStatement statement) {
        return Optional.empty();
    }

    /**
     * @param expr Postfix expression to type check
     * @return Empty optional as postfix expressions don't produce new values
     */
    @Override
    public Optional<IxType> visitPostfixExpr(PostfixExpression expr) {
        expr.realType = expr.expression.accept(this).get();

        if (!(expr.realType instanceof BuiltInType bt && bt.isNumeric())) {
            new CannotPostfixException().send(ixApi, file, expr.expression, expr.operator.source(), expr.realType.getName());
        }
        return Optional.empty();
    }

    /**
     * @param expr Prefix expression to type check
     * @return Optional containing the type of the prefixed expression
     */
    @Override
    public Optional<IxType> visitPrefix(PrefixExpression expr) {
        return expr.right.accept(this);
    }

    /**
     * @param expr Property access expression to type check
     * @return Optional containing the type of the accessed property
     */
    @Override
    public Optional<IxType> visitPropertyAccess(PropertyAccessExpression expr) {
        var t = expr.expression.accept(this);

        var typeChain = new ArrayList<IxType>();

        if (t.isPresent()) {
            var exprType = t.get();
            StructType pointer;
            IxType result = null;
            if (exprType instanceof MonomorphizedStruct mt) {
                pointer = mt.struct;
                typeChain.add(pointer);
                result = pointer;

                result = getTempMSTType(expr, typeChain, pointer, result);
                if (result instanceof GenericType gt) {
                    result = mt.resolved.get(gt.key());
                }
            } else if (exprType instanceof StructType st) {
                pointer = st;
                typeChain.add(pointer);
                result = st;

                result = getTempMSTType(expr, typeChain, pointer, result);
            }
            expr.realType = result;
        } else {
            new MethodNotFoundException().send(ixApi, file, expr.expression, "ree");
        }
        expr.typeChain = typeChain;

        return Optional.ofNullable(expr.realType);
    }

    /**
     * @param statement Return statement to type check
     * @return Empty optional as return statements don't produce values
     */
    @Override
    public Optional<IxType> visitReturnStmt(ReturnStatement statement) {
        var t = statement.expression.accept(this);

        if (t.isPresent()) {
            if (!functionStack.isEmpty()) {
                var newType = t.get();
                var functionType = functionStack.peek();

                if (statement.expression instanceof EmptyListExpression && functionType.returnType instanceof ListType) {
                    functionType.hasReturn2 = true;
                    return Optional.empty();
                }

                if (TypeResolver.typesMatch(functionType.returnType, newType)) {
                }
                else if (functionType.returnType instanceof UnionType ut) {
                    if (!ut.types.contains(newType)) {
                        new ParameterTypeMismatchException().send(ixApi, file, statement.expression, newType, functionType.returnType);
                    }
                }
                else if (functionType.returnType == BuiltInType.VOID) {
                    if (newType != BuiltInType.VOID) {
                        new ReturnTypeMismatchException().send(ixApi, file, statement, functionType.name, functionType.returnType, newType);
                    }
                }
                else {
                    new ReturnTypeMismatchException().send(ixApi, file, statement, functionType.name, functionType.returnType, newType);
                }
            }
        }

        functionStack.peek().hasReturn2 = true;
        return Optional.empty();
    }

    /**
     * @param statement Struct statement to type check
     * @return Empty optional as struct definitions don't produce values
     */
    @Override
    public Optional<IxType> visitStruct(StructStatement statement) {
        var structType = currentContext.getVariableTyped(statement.name.source(), StructType.class);
        if (structType != null) {
            var parametersAfter = new ArrayList<Pair<String, IxType>>();
            CollectionUtil.zip(statement.fields, structType.parameters, (a, b) -> {
                var bType = b.getValue1();
                if (bType instanceof UnknownType ut) {
                    var attempt = currentContext.getVariable(ut.typeName);
                    if (attempt != null) {
                        parametersAfter.add(b.setAt1(attempt));
                    } else if (structType.generics.contains(ut.typeName)) {
                        parametersAfter.add(b.setAt1(new GenericType(ut.typeName)));
                    } else {
                        new IdentifierNotFoundException().send(ixApi, file, a, ut.typeName);
                        parametersAfter.add(b);
                    }
                } else {
                    parametersAfter.add(b);
                }
            });
            structType.parameters.clear();
            structType.parameters.addAll(parametersAfter);

        }

        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitTypeAlias(TypeStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<IxType> visitUnionType(UnionTypeStatement statement) {
        return Optional.empty();
    }

    /**
     * @param statement Variable declaration statement to type check
     * @return Empty optional as variable declarations don't produce values
     */
    @Override
    public Optional<IxType> visitVariable(VariableStatement statement) {

        var expr = statement.expression;
        var t = expr.accept(this);

        if (t.isPresent()) {
            currentContext.setVariableType(statement.name.source(), t.get());
        } else {
            new TypeNotResolvedException().send(ixApi, file, expr, statement.name.source());
        }
        return Optional.empty();
    }

    /**
     * @param statement While loop statement to type check
     * @return Empty optional as loops don't produce values
     */
    @Override
    public Optional<IxType> visitWhile(WhileStatement statement) {
        var childEnvironment = statement.block.context;
        childEnvironment.setParent(currentContext);

        currentContext = childEnvironment;
        statement.condition.accept(this);

        statement.block.accept(this);

        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    /**
     * Resolve unknown types within a union type
     * @param resolvedTypes Set to store resolved types
     * @param ut Union type containing potentially unknown types
     * @param node AST node for error reporting
     */
    private void extractedMethodForUnions(HashSet<IxType> resolvedTypes, UnionType ut, Statement node) {
        for (var type : ut.types) {
            if (type instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.typeName);
                if (attempt != null) {
                    resolvedTypes.add(attempt);
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, node, ukt.typeName);
                }
            } else {
                resolvedTypes.add(type);
            }
        }
        ut.types = resolvedTypes;
    }

    /**
     * Resolve property access chain types for struct types
     * @param expr Property access expression
     * @param typeChain Chain of types encountered during access
     * @param pointer Current struct type being accessed
     * @param result Current result type
     * @return Final resolved type after traversing the property chain
     */
    private IxType getTempMSTType(PropertyAccessExpression expr, ArrayList<IxType> typeChain, StructType pointer, IxType result) {
        for (IdentifierExpression identifier : expr.identifiers) {
            var foundField = pointer.parameters.stream().filter(i -> i.getValue0().equals(identifier.identifier.source())).findAny();
            if (foundField.isPresent()) {
                var pointerCandidate = foundField.get().getValue1();
                if (pointerCandidate instanceof StructType pst) {
                    pointer = pst;
                    typeChain.add(pointer);
                    result = pointerCandidate;
                } else {
                    result = pointerCandidate;
                    typeChain.add(pointerCandidate);

                }

            } else {
                new FieldNotPresentException().send(ixApi, file, identifier, identifier.identifier.source(), pointer.name);
                break;
            }
        }
        return result;
    }

    /**
     * Validate that function call arguments match parameter types
     * @param param Function parameter (name, type)
     * @param arg Argument expression
     * @param argType Resolved type of the argument
     */
    private void typecheckCallParameters(Pair<String, IxType> param, Expression arg, IxType argType) {
        if (argType == BuiltInType.VOID) {
            new VoidUsageException().send(ixApi, file, arg);
        }
        if (!TypeResolver.typesMatch(param.getValue1(), argType)) {
            new ParameterTypeMismatchException().send(ixApi, file, arg, argType.getName(), param.getValue1().getName());
            TypeResolver.typesMatch(param.getValue1(), argType);
            arg.accept(this);
        } else {
            arg.realType = argType;
        }
    }

    /**
     * Update unknown types in function/struct parameters with resolved types
     * @param expr Function call expression
     * @param structType Struct or function type being called
     */
    private void updateUnknownParameters(CallExpression expr, StructType structType) {
        var parametersAfter = new ArrayList<Pair<String, IxType>>();
        CollectionUtil.zip(structType.parameters, expr.arguments, (param, arg) -> {
            if (param.getValue1() instanceof UnknownType ut) {
                var attempt = currentContext.getVariable(ut.typeName);
                if (attempt != null) {
                    parametersAfter.add(param.setAt1(attempt));
                } else {
                    new IdentifierNotFoundException().send(ixApi, file, arg, ut.typeName);
                    parametersAfter.add(param);
                }
            } else {
                parametersAfter.add(param);

            }
        });

        structType.parameters.clear();
        structType.parameters.addAll(parametersAfter);
    }
}