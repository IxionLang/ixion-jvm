package com.kingmang.ixion.codegen;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.exception.*;
import com.kingmang.ixion.lexer.TokenType;
import com.kingmang.ixion.runtime.*;
import com.kingmang.ixion.runtime.CollectionUtil;
import com.kingmang.ixion.typechecker.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.javatuples.Pair;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/*
IMPORTANT !!!
 * Codegen in JVM bytecode is temporarily unavailable.
 * Currently, compilation to Java is being implemented, and later,
 * after the transition to Java 25, codegen in JVM bytecode will be reactivated.
*/

/**
 * Visitor for generating jvm bytecode from Ixion nodes
 * Converts abstract syntax tree to executable bytecode using ASM library
 */
public class CodegenVisitor implements Visitor<Optional<ClassWriter>> {
    public final static int flags = ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS;
    public final static int CLASS_VERSION = 61;
    public final Context rootContext;
    public final IxFile source;
    public final File file;
    public final IxApi ixApi;
    public final ClassWriter cw;
    public final Map<StructType, ClassWriter> structWriters = new HashMap<>();
    private final Stack<DefType> functionStack = new Stack<>();
    public static final Type RuntimeListWrapType = Type.getType(CollectionUtil.IxListWrapper.class);
    public static final Type ArrayListType = Type.getType(ArrayList.class);
    public static final Type IteratorType = Type.getType(Iterator.class);
    public static final Type ObjectType = Type.getType(Object.class);
    public static final String Init = "<init>";
    public static final String Clinit = "<clinit>";
    public Context currentContext;

    /**
     * Constructor for CodegenVisitor
     * @param ixApi The Ixion API instance
     * @param rootContext The root context for symbol resolution
     * @param source The source file being compiled
     * @param cw The ClassWriter for the main class
     */
    public CodegenVisitor(IxApi ixApi, Context rootContext, IxFile source, ClassWriter cw) {
        this.ixApi = ixApi;
        this.rootContext = rootContext;
        this.source = source;
        this.currentContext = this.rootContext;
        this.cw = cw;
        this.file = source.file;
    }

    @Override
    public Optional<ClassWriter> visit(Statement statement) {
        return statement.accept(this);
    }

    /**
     * Visits type alias statement (no code generation needed)
     * @param statement The type alias statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitTypeAlias(TypeAliasStatement statement) {
        return Optional.empty();
    }

    /**
     * Generates code for assignment expressions
     * Handles both variable assignments and property assignments
     * @param expr The assignment expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitAssignExpr(AssignExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;

        if (expr.left instanceof IdentifierExpression id) {
            expr.right.accept(this);
            var index = funcType.localMap.get(id.identifier.source());
            ga.storeLocal(index);
        } else if (expr.left instanceof PropertyAccessExpression pa) {
            var lType = expr.left.realType;
            var rType = expr.right.realType;

            var root = pa.expression;
            root.accept(this);

            var typeChain = pa.typeChain;
            var identifiers = pa.identifiers;
            for (int i = 0; i < typeChain.size() - 2; i++) {
                var current = typeChain.get(i);
                var next = typeChain.get(i + 1);
                var fieldName = identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));
            }
            expr.right.accept(this);

            if (rType instanceof BuiltInType btArg) {
                if (lType instanceof UnionType) {
                    btArg.doBoxing(ga);
                }
            }

            ga.putField(Type.getType(typeChain.get(typeChain.size() - 2).getDescriptor()),
                    identifiers.getLast().identifier.source(),
                    Type.getType(typeChain.getLast().getDescriptor()));

        } else {
            new ImplementationException().send(ixApi, file, expr, "Assignment not implemented for any recipient but identifier yet");
        }
        return Optional.empty();
    }

    /**
     * Handles bad expressions (error cases)
     * @param expr The bad expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    /**
     * Generates code for binary expressions including arithmetic, comparison, and logical operations
     * @param expr The binary expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitBinaryExpr(BinaryExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;
        var left = expr.left;
        var right = expr.right;

        if (expr.realType.equals(BuiltInType.STRING)) {
            ga.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
            ga.visitInsn(Opcodes.DUP);
            ga.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", CodegenVisitor.Init, "()V", false);

            expr.left.accept(this);

            String leftExprDescriptor = expr.left.realType.getDescriptor();
            String descriptor = "(" + leftExprDescriptor + ")Ljava/lang/StringBuilder;";
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false);

            expr.right.accept(this);

            String rightExprDescriptor = expr.right.realType.getDescriptor();
            descriptor = "(" + rightExprDescriptor + ")Ljava/lang/StringBuilder;";
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor, false);
            ga.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        } else {
            switch (expr.operator.type()) {
                case AND -> {
                    Label falseLabel = new Label();
                    Label successLabel = new Label();

                    left.accept(this);
                    ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);

                    right.accept(this);
                    ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);
                    ga.push(true);
                    ga.goTo(successLabel);

                    ga.mark(falseLabel);
                    ga.push(false);

                    ga.mark(successLabel);
                }
                case OR -> {
                    Label falseLabel = new Label();
                    Label successLabel = new Label();
                    Label endLabel = new Label();

                    left.accept(this);
                    ga.ifZCmp(GeneratorAdapter.NE, successLabel);

                    right.accept(this);
                    ga.ifZCmp(GeneratorAdapter.NE, successLabel);
                    ga.goTo(falseLabel);

                    ga.mark(successLabel);
                    ga.push(true);
                    ga.goTo(endLabel);

                    ga.mark(falseLabel);
                    ga.push(false);

                    ga.mark(endLabel);
                }
                case XOR -> {
                    left.accept(this);
                    right.accept(this);
                    ga.visitInsn(Opcodes.IXOR);
                }
                case EQUAL, NOTEQUAL, LT, GT, LE, GE -> {
                    var cmpType = castAndAccept(ga, left, right, this);

                    Label endLabel = new Label();
                    Label falseLabel = new Label();

                    int opcode = switch (expr.operator.type()) {
                        case EQUAL -> GeneratorAdapter.NE;
                        case NOTEQUAL -> GeneratorAdapter.EQ;
                        case LT -> GeneratorAdapter.GE;
                        case GT -> GeneratorAdapter.LE;
                        case LE -> GeneratorAdapter.GT;
                        case GE -> GeneratorAdapter.LT;
                        default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
                    };

                    ga.ifCmp(cmpType, opcode, falseLabel);
                    ga.push(true);
                    ga.goTo(endLabel);

                    ga.mark(falseLabel);
                    ga.push(false);
                    ga.mark(endLabel);
                }
                case MOD -> {
                    var cmpType = castAndAccept(ga, left, right, this);

                    if (cmpType == Type.DOUBLE_TYPE) {
                        ga.visitInsn(Opcodes.DREM);
                    } else if (cmpType == Type.INT_TYPE) {
                        ga.visitInsn(Opcodes.IREM);
                    } else if (cmpType == Type.FLOAT_TYPE) {
                        ga.visitInsn(Opcodes.FREM);
                    }
                }
                case POW -> {}
                case ADD, SUB, MUL, DIV -> {
                    if (left.realType.equals(right.realType)) {
                        left.accept(this);
                        right.accept(this);
                    } else {
                        if (left.realType == BuiltInType.INT && right.realType == BuiltInType.FLOAT) {
                            left.accept(this);
                            ga.visitInsn(Opcodes.I2F);
                            right.accept(this);
                            expr.realType = BuiltInType.FLOAT;
                        } else if (left.realType == BuiltInType.FLOAT && right.realType == BuiltInType.INT) {
                            left.accept(this);
                            right.accept(this);
                            ga.visitInsn(Opcodes.I2F);
                            expr.realType = BuiltInType.FLOAT;
                        } else if (left.realType == BuiltInType.INT && right.realType == BuiltInType.DOUBLE) {
                            left.accept(this);
                            ga.visitInsn(Opcodes.I2D);
                            right.accept(this);
                            expr.realType = BuiltInType.DOUBLE;
                        } else if (left.realType == BuiltInType.DOUBLE && right.realType == BuiltInType.INT) {
                            left.accept(this);
                            right.accept(this);
                            ga.visitInsn(Opcodes.I2D);
                            expr.realType = BuiltInType.DOUBLE;
                        }
                    }
                    if (expr.realType instanceof BuiltInType bt) {
                        int op = switch (expr.operator.type()) {
                            case ADD -> bt.getAddOpcode();
                            case SUB -> bt.getSubtractOpcode();
                            case MUL -> bt.getMultiplyOpcode();
                            case DIV -> bt.getDivideOpcode();
                            case LT, GT, LE, GE -> 0;
                            default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
                        };
                        ga.visitInsn(op);
                    } else {
                        IxApi.exit("need a test case here", 452);
                    }
                }
                case null, default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
            }
        }
        return Optional.empty();
    }

    /**
     * Visits block statements and generates code for each statement in sequence
     * @param statement The block statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitBlockStmt(BlockStatement statement) {
        for (var stmt : statement.statements) stmt.accept(this);
        return Optional.empty();
    }

    /**
     * Generates code for function call expressions
     * Handles both internal function calls and external API calls
     * @param expr The call expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitCall(CallExpression expr) {
        var funcType = functionStack.peek();

        if (expr.item instanceof IdentifierExpression identifier) {
            expr.item.realType = currentContext.getVariable(identifier.identifier.source());
        }

        if (expr.item.realType instanceof DefType callType) {
            if (callType.glue) {
                String owner = callType.owner;
                String name = callType.name;
                if (callType.isPrefixed) name = "_" + name;

                var params = callType.parameters.stream().map(arg -> Pair.with(arg.getValue1().getName(), arg.getValue1())).collect(Collectors.toList());
                IxType returnType = callType.returnType;
                String methodDescriptor = TypeUtils.getMethodDescriptor(params, returnType);

                CollectionUtil.zip(params, expr.arguments, (param, arg) -> {
                    arg.accept(this);
                    if (arg.realType instanceof BuiltInType btArg) {
                        if (param.getValue1() instanceof ExternalType(Class<?> foundClass) && foundClass.equals(Object.class)) {
                            btArg.doBoxing(funcType.ga);
                        }
                    }
                });

                funcType.ga.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, methodDescriptor, false);
            } else {
                var ga = funcType.ga;
                CollectionUtil.zip(callType.parameters, expr.arguments, (param, arg) -> {
                    arg.accept(this);
                    if (arg.realType instanceof BuiltInType btArg) {
                        if (param.getValue1() instanceof ExternalType(Class<?> foundClass) && foundClass.equals(Object.class)) {
                            btArg.doBoxing(ga);
                        } else if (param.getValue1() instanceof UnionType ut) {
                            btArg.doBoxing(funcType.ga);
                        }
                    }
                });

                var specialization = callType.buildSpecialization(expr.arguments);
                var returnType = callType.returnType;
                if (returnType instanceof GenericType(String key)) {
                    returnType = DefType.getSpecializedType(specialization, key);
                }

                var parameters = callType.buildParametersFromSpecialization(specialization);
                String descriptor = TypeUtils.getMethodDescriptor(parameters, returnType);

                String name = "_" + callType.name;
                String owner = FilenameUtils.removeExtension(source.getFullRelativePath());

                if (callType.external != null) {
                    owner = callType.external.getFullRelativePath();
                }

                funcType.ga.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
            }
        } else if (expr.item.realType instanceof StructType st) {
            var ga = funcType.ga;

            ga.newInstance(Type.getType("L" + st.qualifiedName + ";"));
            ga.visitInsn(Opcodes.DUP);

            StringBuilder typeDescriptor = new StringBuilder();
            CollectionUtil.zip(st.parameters, expr.arguments, (param, arg) -> {
                arg.accept(this);
                var paramType = param.getValue1();
                if (paramType instanceof UnionType ut || paramType instanceof GenericType) {
                    typeDescriptor.append(paramType.getDescriptor());
                    if (arg.realType instanceof BuiltInType btArg) {
                        btArg.doBoxing(ga);
                    }
                } else {
                    typeDescriptor.append(arg.realType.getDescriptor());
                }
            });

            ga.invokeConstructor(Type.getType("L" + st.qualifiedName + ";"), new Method(Init, "(" + typeDescriptor + ")V"));
        } else {
            System.err.println("Bad!");
            System.exit(43);
        }

        return Optional.empty();
    }

    /**
     * Handles empty expressions (no operation)
     * @param empty The empty expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    /**
     * Generates code for empty list expressions
     * Creates a new ArrayList instance
     * @param emptyList The empty list expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitEmptyList(EmptyListExpression emptyList) {
        var ga = functionStack.peek().ga;

        ga.newInstance(ArrayListType);
        ga.dup();
        ga.invokeConstructor(ArrayListType, new Method(Init, "()V"));
        return Optional.empty();
    }

    /**
     * Visits enum statements (not implemented)
     * @param statement The enum statement
     * @return null
     */
    @Override
    public Optional<ClassWriter> visitEnum(EnumStatement statement) {
        return Optional.empty();
    }

    /**
     * Visits export statements by delegating to the contained statement
     * @param statement The export statement
     * @return Result of visiting the contained statement
     */
    @Override
    public Optional<ClassWriter> visitExport(ExportStatement statement) {
        return statement.stmt.accept(this);
    }

    /**
     * Visits expression statements by visiting the contained expression
     * @param statement The expression statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitExpressionStmt(ExpressionStatement statement) {
        statement.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Generates code for for-loop statements
     * Creates iterator-based loop structure
     * @param statement The for statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitFor(ForStatement statement) {
        var funcType = functionStack.peek();
        var ga = functionStack.peek().ga;
        currentContext = statement.block.context;
        var startLabel = new Label();
        var endLabel = new Label();

        if (statement.expression instanceof IdentifierExpression id) {
            ga.mark(startLabel);
            statement.expression.accept(this);
        } else {
            statement.expression.accept(this);
            statement.localExprIndex = ga.newLocal(IteratorType);
            funcType.localMap.put("______", statement.localExprIndex);
            ga.storeLocal(statement.localExprIndex, IteratorType);
            ga.mark(startLabel);
            ga.loadLocal(statement.localExprIndex);
        }

        ga.invokeInterface(IteratorType, new Method("hasNext", "()Z"));
        ga.visitJumpInsn(Opcodes.IFEQ, endLabel);

        if (statement.expression instanceof IdentifierExpression id) {
            statement.expression.accept(this);
        } else {
            ga.loadLocal(statement.localExprIndex);
        }
        ga.invokeInterface(IteratorType, new Method("next", "()Ljava/lang/Object;"));
        BuiltInType.INT.doUnboxing(ga);
        statement.localNameIndex = ga.newLocal(Type.getType(BuiltInType.INT.getDescriptor()));
        funcType.localMap.put(statement.name.source(), statement.localNameIndex);
        funcType.ga.storeLocal(statement.localNameIndex, Type.getType(BuiltInType.INT.getDescriptor()));

        statement.block.accept(this);

        ga.goTo(startLabel);
        ga.mark(endLabel);
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    /**
     * Generates code for function statements
     * Creates method definitions with proper signatures and bodies
     * @param statement The function statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitFunctionStmt(FunctionStatement statement) {
        var funcType = currentContext.getVariableTyped(statement.name.source(), DefType.class);
        functionStack.add(funcType);
        var childEnvironment = statement.body.context;

        String name = "_" + funcType.name;
        var access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC;

        if (funcType.hasGenerics()) {
            for (Map<String, IxType> specialization : funcType.specializations) {
                funcType.currentSpecialization = specialization;
                var returnType = funcType.returnType;
                if (returnType instanceof GenericType(String key)) {
                    returnType = DefType.getSpecializedType(specialization, key);
                }

                var parameters = funcType.buildParametersFromSpecialization(specialization);
                String descriptor = TypeUtils.getMethodDescriptor(parameters, returnType);

                var mv = cw.visitMethod(access, name, descriptor, null, null);
                funcType.ga = new GeneratorAdapter(mv, access, name, descriptor);
                for (int i = 0; i < funcType.parameters.size(); i++) {
                    var param = funcType.parameters.get(i);
                    funcType.argMap.put(param.getValue0(), i);
                }

                currentContext = childEnvironment;
                statement.body.accept(this);
                funcType.ga.endMethod();
                currentContext = currentContext.getParent();
            }
            functionStack.pop();
        } else {
            String descriptor = TypeUtils.getMethodDescriptor(funcType.parameters, funcType.returnType);
            if (funcType.name.equals("main")) {
                name = "main";
                descriptor = "([Ljava/lang/String;)V";
            }
            var mv = cw.visitMethod(access, name, descriptor, null, null);
            funcType.ga = new GeneratorAdapter(mv, access, name, descriptor);
            for (int i = 0; i < funcType.parameters.size(); i++) {
                var param = funcType.parameters.get(i);
                funcType.argMap.put(param.getValue0(), i);
            }

            currentContext = childEnvironment;
            statement.body.accept(this);
            funcType.ga.endMethod();
            currentContext = currentContext.getParent();
            functionStack.pop();
        }
        return Optional.empty();
    }

    /**
     * Visits grouping expressions by visiting the contained expression
     * @param expr The grouping expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitGroupingExpr(GroupingExpression expr) {
        expr.expression.accept(this);
        return Optional.empty();
    }

    /**
     * Generates code for identifier expressions
     * Loads variables from local storage or function arguments
     * @param expr The identifier expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitIdentifierExpr(IdentifierExpression expr) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;
        var type = currentContext.getVariable(expr.identifier.source());

        if (type instanceof GenericType(String key)) {
            type = funcType.currentSpecialization.get(key);
        }

        expr.realType = type;

        int index;
        String source = expr.identifier.source();
        if (funcType.localMap.containsKey(source)) {
            index = funcType.localMap.get(source);
            ga.loadLocal(index, Type.getType(type.getDescriptor()));
        } else {
            index = funcType.argMap.getOrDefault(source, -1);
            if (index == -1) {
                new IdentifierNotFoundException().send(ixApi, file, expr, source);
                return Optional.empty();
            }
            ga.loadArg(index);
        }

        return Optional.empty();
    }

    /**
     * Generates code for if statements with optional else branches
     * Creates conditional jump instructions
     * @param statement The if statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitIf(IfStatement statement) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;
        Label endLabel = new Label();
        Label falseLabel = new Label();

        statement.condition.accept(this);

        currentContext = statement.trueBlock.context;
        ga.ifZCmp(GeneratorAdapter.EQ, falseLabel);
        statement.trueBlock.accept(this);
        ga.goTo(endLabel);
        ga.mark(falseLabel);
        if (statement.falseStatement != null) statement.falseStatement.accept(this);
        ga.mark(endLabel);
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    /**
     * Visits use statements (no code generation needed)
     * @param statement The use statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitUse(UseStatement statement) {
        return Optional.empty();
    }

    /**
     * Visits index access expressions (not implemented)
     * @param expr The index access expression
     * @throws NotImplementedException Always thrown
     */
    @Override
    public Optional<ClassWriter> visitIndexAccess(IndexAccessExpression expr) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Generates code for literal expressions
     * Pushes constant values onto the stack
     * @param expr The literal expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitLiteralExpr(LiteralExpression expr) {
        if (expr.realType instanceof BuiltInType bt) {
            var transformed = TypeResolver.getValueFromString(expr.literal.source(), TypeUtils.getFromToken(expr.literal.type()));
            var ga = functionStack.peek().ga;
            switch (bt) {
                case INT -> ga.push((int) transformed);
                case FLOAT -> ga.push((float) transformed);
                case DOUBLE -> ga.push((double) transformed);
                case BOOLEAN -> ga.push((boolean) transformed);
                case STRING -> ga.push((String) transformed);
            }
        } else {
            new ImplementationException().send(ixApi, source.file, expr, "This should never happen. All literals should be builtin, for now.");
        }
        return Optional.empty();
    }

    /**
     * Generates code for list literal expressions
     * Creates list wrapper objects with elements
     * @param expr The list literal expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitLiteralList(LiteralListExpression expr) {
        var ga = functionStack.peek().ga;

        ga.newInstance(RuntimeListWrapType);
        ga.dup();
        ga.push(((ListType) expr.realType).contentType().getName());
        ga.invokeConstructor(RuntimeListWrapType, new Method(Init, "(Ljava/lang/String;)V"));
        ga.dup();
        ga.invokeVirtual(RuntimeListWrapType, new Method("list", "()Ljava/util/ArrayList;"));

        for (var entry : expr.entries) {
            ga.dup();
            entry.accept(this);
            var rt = entry.realType;
            if (rt instanceof BuiltInType bt) {
                bt.doBoxing(ga);
            }
            ga.invokeVirtual(ArrayListType, new Method("add", "(Ljava/lang/Object;)Z"));
            ga.pop();
        }
        ga.pop();
        return Optional.empty();
    }

    /**
     * Generates code for match statements (pattern matching)
     * Converts to instanceof checks and type casts
     * @param statement The match statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitMatch(final MatchStatement statement) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;

        statement.expression.accept(this);
        int localExprIndex = ga.newLocal(ObjectType);
        var s = ":)";
        funcType.localMap.put(s, localExprIndex);
        ga.storeLocal(localExprIndex);

        for (TypeStatement typeStmt : statement.cases.keySet()) {
            var pair = statement.cases.get(typeStmt);
            var scopedName = pair.getValue0();
            var block = pair.getValue1();
            var t = statement.types.get(typeStmt);
            if (t instanceof UnknownType ukt) {
                var attempt = currentContext.getVariable(ukt.typeName);
                if (attempt != null) {
                    t = attempt;
                }
            }

            var end = new Label();
            ga.loadLocal(localExprIndex);

            if (t instanceof ListType(IxType contentType)) {
                ga.instanceOf(RuntimeListWrapType);
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);
                ga.checkCast(RuntimeListWrapType);
                ga.storeLocal(localExprIndex);

                ga.loadLocal(localExprIndex);
                funcType.ga.invokeVirtual(RuntimeListWrapType, new Method("name", "()Ljava/lang/String;"));
                ga.push(contentType.getName());
                funcType.ga.invokeVirtual(Type.getType(String.class), new Method("equals", "(Ljava/lang/Object;)Z"));
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);
            } else {
                Type typeClass;
                if (t instanceof BuiltInType bt) {
                    typeClass = Type.getType(t.getTypeClass());
                } else if (t instanceof StructType st) {
                    typeClass = Type.getType("L" + st.qualifiedName + ";");
                } else {
                    typeClass = Type.getType(t.getDescriptor());
                }
                ga.instanceOf(typeClass);
                ga.visitJumpInsn(Opcodes.IFEQ, end);
                ga.loadLocal(localExprIndex);
                ga.checkCast(typeClass);
            }

            if (t instanceof BuiltInType bt && bt.isNumeric()) {
                bt.unboxNoCheck(ga);
                int localPrimitiveType = ga.newLocal(Type.getType(TypeUtils.convert(bt.getTypeClass())));
                ga.storeLocal(localPrimitiveType);
                funcType.localMap.put(scopedName, localPrimitiveType);
            } else {
                int localObjectType = ga.newLocal(ObjectType);
                ga.storeLocal(localObjectType);
                funcType.localMap.put(scopedName, localObjectType);
            }

            currentContext = block.context;
            block.accept(this);
            currentContext = currentContext.getParent();

            ga.mark(end);
        }

        return Optional.empty();
    }

    /**
     * Visits module access expressions (no code generation needed)
     * @param expr The module access expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitModuleAccess(ModuleAccessExpression expr) {
        return Optional.empty();
    }

    /**
     * Visits parameter statements (not implemented)
     * @param statement The parameter statement
     * @throws NotImplementedException Always thrown
     */
    @Override
    public Optional<ClassWriter> visitParameterStmt(ParameterStatement statement) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Generates code for postfix expressions (increment/decrement)
     * @param expr The postfix expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitPostfixExpr(PostfixExpression expr) {
        var ga = functionStack.peek().ga;
        expr.expression.accept(this);
        if (expr.realType instanceof BuiltInType bt) {
            bt.pushOne(ga);

            int op = switch (expr.operator.type()) {
                case PLUSPLUS -> bt.getAddOpcode();
                case MINUSMINUS -> bt.getSubtractOpcode();
                default -> throw new IllegalStateException("Unexpected value: " + expr.operator.type());
            };

            ga.visitInsn(op);
            if (expr.expression instanceof IdentifierExpression eid) {
                ga.storeLocal(functionStack.peek().localMap.get(eid.identifier.source()));
            }
        } else {
            IxApi.exit("postfix only works with builtin types", 49);
        }
        return Optional.empty();
    }

    /**
     * Generates code for prefix expressions (unary operators)
     * @param expr The prefix expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitPrefix(PrefixExpression expr) {
        var ga = functionStack.peek().ga;

        expr.right.accept(this);

        var t = expr.right.realType;
        if (expr.operator.type() == TokenType.SUB && t instanceof BuiltInType bt) {
            ga.visitInsn(bt.getNegOpcode());
            expr.realType = t;
        }
        return Optional.empty();
    }

    /**
     * Generates code for property access expressions (field access)
     * Handles chained property accesses
     * @param expr The property access expression
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitPropertyAccess(PropertyAccessExpression expr) {
        var ga = functionStack.peek().ga;
        var t = expr.realType;

        var root = expr.expression;
        var rootType = root.realType;

        if (rootType instanceof MonomorphizedStruct mst) {
            root.accept(this);

            for (int i = 0; i < expr.typeChain.size() - 1; i++) {
                var current = expr.typeChain.get(i);
                var next = expr.typeChain.get(i + 1);

                var key = ((GenericType) next).key();
                var r = mst.resolved.get(key);

                var fieldName = expr.identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));
                ga.checkCast(Type.getType(r.getDescriptor()));
            }
        } else {
            root.accept(this);

            for (int i = 0; i < expr.typeChain.size() - 1; i++) {
                var current = expr.typeChain.get(i);
                var next = expr.typeChain.get(i + 1);
                var fieldName = expr.identifiers.get(i).identifier.source();
                ga.getField(Type.getType(current.getDescriptor()), fieldName, Type.getType(next.getDescriptor()));
            }
        }

        return Optional.empty();
    }

    /**
     * Generates code for return statements
     * Handles both void and value returns with boxing if needed
     * @param statement The return statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitReturnStmt(ReturnStatement statement) {
        var funcType = functionStack.peek();
        if (!(statement.expression instanceof EmptyExpression)) {
            statement.expression.accept(this);

            if (funcType.returnType instanceof UnionType && statement.expression.realType instanceof BuiltInType bt) {
                bt.doBoxing(funcType.ga);
            }

            var returnType = funcType.returnType;
            if (returnType instanceof GenericType(String key)) {
                returnType = DefType.getSpecializedType(funcType.currentSpecialization, key);
            }

            funcType.ga.visitInsn(returnType.getReturnOpcode());
        } else {
            funcType.ga.visitInsn(Opcodes.RETURN);
        }
        return Optional.empty();
    }

    /**
     * Generates code for struct definitions
     * Creates inner classes with fields and constructors
     * @param statement The struct statement
     * @return Optional containing the ClassWriter for the struct
     */
    @Override
    public Optional<ClassWriter> visitStruct(StructStatement statement) {
        var innerCw = new ClassWriter(CodegenVisitor.flags);
        var structType = currentContext.getVariableTyped(statement.name.source(), StructType.class);

        String name = structType.name;
        String innerName = source.getFullRelativePath() + "$" + name;

        innerCw.visit(CodegenVisitor.CLASS_VERSION, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, innerName, null, "java/lang/Object", null);
        cw.visitInnerClass(innerName, source.getFullRelativePath(), name, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC);
        innerCw.visitOuterClass(source.getFullRelativePath(), name, "()V");

        StringBuilder constructorDescriptor = new StringBuilder();

        for (var pair : structType.parameters) {
            IxType type = pair.getValue1();
            var descriptor = type.getDescriptor();
            String n = pair.getValue0();

            var fieldVisitor = innerCw.visitField(Opcodes.ACC_PUBLIC, n, descriptor, null, null);
            fieldVisitor.visitEnd();

            constructorDescriptor.append(descriptor);
        }

        var descriptor = "(" + constructorDescriptor + ")V";
        MethodVisitor _mv = innerCw.visitMethod(Opcodes.ACC_PUBLIC, Init, descriptor, null, null);
        var ga = new GeneratorAdapter(_mv, Opcodes.ACC_PUBLIC, Init, descriptor);

        String ownerInternalName = source.getFullRelativePath() + "$" + name;

        ga.loadThis();
        ga.invokeConstructor(ObjectType, new Method(Init, "()V"));

        for (int i = 0; i < structType.parameters.size(); i++) {
            IxType type = structType.parameters.get(i).getValue1();
            descriptor = type.getDescriptor();
            String n = structType.parameters.get(i).getValue0();
            ga.visitVarInsn(Opcodes.ALOAD, 0);
            ga.loadArg(i);

            ga.visitFieldInsn(Opcodes.PUTFIELD, ownerInternalName, n, descriptor);
        }

        ga.returnValue();
        ga.endMethod();

        BytecodeGenerator.addToString(innerCw, structType, constructorDescriptor.toString(), ownerInternalName);

        structWriters.put(structType, innerCw);

        return Optional.of(innerCw);
    }

    /**
     * Visits type alias statements (not implemented)
     * @param statement The type statement
     * @throws NotImplementedException Always thrown
     */
    @Override
    public Optional<ClassWriter> visitTypeAlias(TypeStatement statement) {
        throw new NotImplementedException("method not implemented");
    }

    /**
     * Visits union type statements (no code generation needed)
     * @param statement The union type statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitUnionType(UnionTypeStatement statement) {
        return Optional.empty();
    }

    /**
     * Generates code for variable declarations
     * Allocates local storage and stores initial values
     * @param statement The variable statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitVariable(VariableStatement statement) {
        var funcType = functionStack.peek();
        statement.expression.accept(this);

        var type = currentContext.getVariable(statement.identifier());
        if (type instanceof GenericType(String key)) {
            type = funcType.currentSpecialization.get(key);
        }
        statement.localIndex = funcType.ga.newLocal(Type.getType(type.getDescriptor()));
        funcType.localMap.put(statement.identifier(), statement.localIndex);
        funcType.ga.storeLocal(statement.localIndex, Type.getType(type.getDescriptor()));

        if (statement.expression instanceof PostfixExpression pe) {
            pe.localIndex = statement.localIndex;
        }

        return Optional.empty();
    }

    /**
     * Generates code for while loop statements
     * Creates loop structure with condition checking
     * @param statement The while statement
     * @return Empty optional
     */
    @Override
    public Optional<ClassWriter> visitWhile(WhileStatement statement) {
        var funcType = functionStack.peek();
        var ga = funcType.ga;
        Label endLabel = new Label();
        Label startLabel = new Label();

        ga.mark(startLabel);
        statement.condition.accept(this);

        currentContext = statement.block.context;
        ga.ifZCmp(GeneratorAdapter.EQ, endLabel);
        statement.block.accept(this);
        ga.goTo(startLabel);
        ga.mark(endLabel);
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    /**
     * Helper method to cast and accept expressions for binary operations
     * Handles type widening and casting for compatible types
     * @param ga The generator adapter
     * @param left The left expression
     * @param right The right expression
     * @param visitor The codegen visitor
     * @return The common type after casting
     */
    private static Type castAndAccept(GeneratorAdapter ga, Expression left, Expression right, CodegenVisitor visitor) {
        int lWide = BuiltInType.widenings.getOrDefault((BuiltInType) left.realType, -1);
        int rWide = BuiltInType.widenings.getOrDefault((BuiltInType) right.realType, -1);
        var lType = Type.getType(left.realType.getDescriptor());
        var rType = Type.getType(right.realType.getDescriptor());

        var cmpType = lType;

        if (lWide != -1 && rWide != -1) {
            if (lWide > rWide) {
                left.accept(visitor);
                right.accept(visitor);
                ga.cast(rType, lType);
            } else if (lWide < rWide) {
                left.accept(visitor);
                ga.cast(lType, rType);
                right.accept(visitor);
                cmpType = rType;
            } else {
                left.accept(visitor);
                right.accept(visitor);
            }
        } else {
            left.accept(visitor);
            right.accept(visitor);
        }
        return cmpType;
    }
}