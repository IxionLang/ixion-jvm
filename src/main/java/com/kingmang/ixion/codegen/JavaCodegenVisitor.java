package com.kingmang.ixion.codegen;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.runtime.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class JavaCodegenVisitor implements Visitor<Optional<String>> {
    private final IxApi ixApi;
    private final IxFile source;
    private Context currentContext;
    private final StringBuilder output = new StringBuilder();
    private int indentLevel = 0;
    private final Stack<DefType> functionStack = new Stack<>();
    private final Map<String, String> structClasses = new HashMap<>();
    private final Map<DefType, Map<String, Integer>> localMaps = new HashMap<>();

    public JavaCodegenVisitor(IxApi ixApi, IxFile source) {
        this.ixApi = ixApi;
        this.source = source;
        this.currentContext = source.rootContext;
    }

    private void indent() {
        output.append("    ".repeat(Math.max(0, indentLevel)));
    }

    private void println(String line) {
        indent();
        output.append(line).append("\n");
    }

    private void print(String text) {
        output.append(text);
    }

    public String getGeneratedCode() {
        return output.toString();
    }

    @Override
    public Optional<String> visit(Statement statement) {
        return statement.accept(this);
    }

    @Override
    public Optional<String> visitAssignExpr(AssignExpression expr) {
        if (expr.left instanceof IdentifierExpression id) {
            print(id.identifier.source() + " = ");
            expr.right.accept(this);
        } else if (expr.left instanceof PropertyAccessExpression pa) {
            pa.accept(this);
            print(" = ");
            expr.right.accept(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBad(BadExpression expr) {
        print("/* ERROR: Bad expression */");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBinaryExpr(BinaryExpression expr) {
        expr.left.accept(this);

        String operator = switch (expr.operator.type()) {
            case AND -> " && ";
            case OR -> " || ";
            case EQUAL -> " == ";
            case NOTEQUAL -> " != ";
            case LT -> " < ";
            case GT -> " > ";
            case LE -> " <= ";
            case GE -> " >= ";
            case ADD -> " + ";
            case SUB -> " - ";
            case MUL -> " * ";
            case DIV -> " / ";
            case MOD -> " % ";
            case XOR -> " ^ ";
            default -> " " + expr.operator.source() + " ";
        };

        print(operator);
        expr.right.accept(this);
        return Optional.empty();
    }

    @Override
    public Optional<String> visitCall(CallExpression expr) {
        if (expr.item instanceof IdentifierExpression identifier) {
            expr.item.realType = currentContext.getVariable(identifier.identifier.source());
        }

        if (expr.item.realType instanceof DefType callType) {
            if (callType.glue) {
                String owner = callType.owner.replace('/', '.');
                String name = callType.name;
                if (callType.isPrefixed) name = "_" + name;

                print(owner + "." + name + "(");
            } else {
                String name = "_" + callType.name;

                if (callType.external != null && !callType.external.equals(source)) {
                    String className = IxApi.getClassName(callType.external);
                    print(className + "." + name + "(");
                } else {
                    print(name + "(");
                }
            }

            for (int i = 0; i < expr.arguments.size(); i++) {
                if (i > 0) print(", ");
                expr.arguments.get(i).accept(this);
            }
            print(")");
        } else if (expr.item.realType instanceof StructType st) {
            print("new " + st.name + "(");
            for (int i = 0; i < expr.arguments.size(); i++) {
                if (i > 0) print(", ");
                expr.arguments.get(i).accept(this);
            }
            print(")");
        } else {
            expr.item.accept(this);
            print("(");
            for (int i = 0; i < expr.arguments.size(); i++) {
                if (i > 0) print(", ");
                expr.arguments.get(i).accept(this);
            }
            print(")");
        }

        return Optional.empty();
    }


    @Override
    public Optional<String> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitEmptyList(EmptyListExpression emptyList) {
        print("new java.util.ArrayList<>()");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitGroupingExpr(GroupingExpression expr) {
        print("(");
        expr.expression.accept(this);
        print(")");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitIdentifierExpr(IdentifierExpression expr) {
        print(expr.identifier.source());
        return Optional.empty();
    }

    @Override
    public Optional<String> visitIndexAccess(IndexAccessExpression expr) {
        expr.left.accept(this);
        print(".get(");
        expr.right.accept(this);
        print(")");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitLiteralExpr(LiteralExpression expr) {
        if (expr.realType instanceof BuiltInType bt) {
            switch (bt) {
                case STRING -> print("\"" + expr.literal.source().replace("\"", "\\\"") + "\"");
                case BOOLEAN -> print(expr.literal.source());
                default -> print(expr.literal.source());
            }
        } else {
            print(expr.literal.source());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> visitLiteralList(LiteralListExpression expr) {
        if (expr.entries.isEmpty()) {
            print("new java.util.ArrayList<>()");
        } else {
            IxType elementType = expr.entries.getFirst().realType;
            String wrapperType = getWrapperTypeName(elementType);

            print("java.util.Arrays.<" + wrapperType + ">asList(");
            for (int i = 0; i < expr.entries.size(); i++) {
                if (i > 0) print(", ");
                expr.entries.get(i).accept(this);
            }
            print(")");
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> visitModuleAccess(ModuleAccessExpression expr) {
        expr.foreign.accept(this);
        print(".");
        expr.identifier.accept(this);
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPostfixExpr(PostfixExpression expr) {
        expr.expression.accept(this);
        print(expr.operator.source());
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPrefix(PrefixExpression expr) {
        print(expr.operator.source());
        expr.right.accept(this);
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPropertyAccess(PropertyAccessExpression expr) {
        expr.expression.accept(this);
        for (var identifier : expr.identifiers) {
            print("." + identifier.identifier.source());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> visitTypeAlias(TypeAliasStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBlockStmt(BlockStatement statement) {
        println("{");
        indentLevel++;
        for (var stmt : statement.statements) {
            stmt.accept(this);
        }
        indentLevel--;
        println("}");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitEnum(EnumStatement statement) {
        indent();
        print("enum " + statement.name.source() + " {");
        for (int i = 0; i < statement.values.size(); i++) {
            if (i > 0) print(", ");
            print(statement.values.get(i).source());
        }
        println("}");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitExport(ExportStatement statement) {
        return statement.stmt.accept(this);
    }

    @Override
    public Optional<String> visitExpressionStmt(ExpressionStatement statement) {
        indent();
        statement.expression.accept(this);
        println(";");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitFor(ForStatement statement) {
        indent();

        IxType elementType = BuiltInType.ANY;
        if (statement.expression.realType instanceof ListType(IxType contentType)) {
            elementType = contentType;
        } else if (statement.expression instanceof IdentifierExpression idExpr) {
            var varType = currentContext.getVariable(idExpr.identifier.source());
            if (varType instanceof ListType(IxType contentType)) {
                elementType = contentType;
            }
        }

        String javaElementType = getJavaTypeName(elementType);

        if (elementType instanceof BuiltInType builtIn && builtIn.isNumeric()) {
            String wrapperType = getWrapperTypeName(elementType);
            print("for (java.util.Iterator<" + wrapperType + "> iter = ");
            statement.expression.accept(this);
            println(".iterator(); iter.hasNext(); ) {");

            indentLevel++;
            indent();
            print(javaElementType + " " + statement.name.source() + " = ");
            switch (builtIn) {
                case INT -> print("iter.next().intValue()");
                case FLOAT -> print("iter.next().floatValue()");
                case DOUBLE -> print("iter.next().doubleValue()");
                case BOOLEAN -> print("iter.next().booleanValue()");
                default -> print("iter.next()");
            }
            println(";");

        } else {
            print("for (" + javaElementType + " " + statement.name.source() + " : ");
            statement.expression.accept(this);
            println(") {");
            indentLevel++;
        }

        currentContext = statement.block.context;
        statement.block.accept(this);
        indentLevel--;
        println("}");
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    @Override
    public Optional<String> visitFunctionStmt(DefStatement statement) {
        var funcType = currentContext.getVariableTyped(statement.name.source(), DefType.class);
        functionStack.push(funcType);
        localMaps.put(funcType, new HashMap<>());

        indent();
        print("public static ");

        if (funcType.name.equals("main")) {
            print("void main(String[] args)");
        } else {
            String returnType = getJavaTypeName(funcType.returnType);
            String functionName = funcType.glue ? funcType.name : "_" + funcType.name;

            print(returnType + " " + functionName + "(");

            for (int i = 0; i < funcType.parameters.size(); i++) {
                var param = funcType.parameters.get(i);
                if (i > 0) print(", ");
                String paramType = getJavaTypeName(param.getValue1());
                print(paramType + " " + param.getValue0());
            }
            print(")");
        }
        println(" {");

        indentLevel++;
        currentContext = statement.body.context;
        statement.body.accept(this);

        if (!funcType.name.equals("main") && !hasReturnStatement(statement.body) && !funcType.returnType.equals(BuiltInType.VOID)) {
            indent();
            String defaultValue = getDefaultValue(funcType.returnType);
            println("return " + defaultValue + ";");
        }

        indentLevel--;
        println("}");

        functionStack.pop();
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    private boolean hasReturnStatement(BlockStatement body) {
        for (var stmt : body.statements) {
            if (stmt instanceof ReturnStatement) {
                return true;
            } else if (stmt instanceof BlockStatement block) {
                if (hasReturnStatement(block)) return true;
            } else if (stmt instanceof IfStatement ifStmt) {
                boolean trueBranch = hasReturnStatement(ifStmt.trueBlock);
                boolean falseBranch = hasReturnStatementInStatement(ifStmt.falseStatement);
                if (trueBranch && falseBranch) return true;
            }
        }
        return false;
    }

    private boolean hasReturnStatementInStatement(Statement stmt) {
        if (stmt == null) return false;
        if (stmt instanceof ReturnStatement) return true;
        if (stmt instanceof BlockStatement block) return hasReturnStatement(block);
        if (stmt instanceof IfStatement ifStmt) {
            boolean trueBranch = hasReturnStatement(ifStmt.trueBlock);
            boolean falseBranch = hasReturnStatementInStatement(ifStmt.falseStatement);
            return trueBranch && falseBranch;
        }
        return false;
    }

    private String getWrapperTypeName(IxType type) {
        if (type instanceof BuiltInType builtIn) {
            return switch (builtIn) {
                case INT -> "Integer";
                case FLOAT -> "Float";
                case DOUBLE -> "Double";
                case BOOLEAN -> "Boolean";
                case STRING -> "String";
                case VOID -> "Void";
                case ANY -> "Object";
            };
        } else if (type instanceof ListType(IxType contentType)) {
            String elementType = getWrapperTypeName(contentType);
            return "java.util.List<" + elementType + ">";
        }
        return getJavaTypeName(type);
    }

    @Override
    public Optional<String> visitIf(IfStatement statement) {
        indent();
        print("if (");
        statement.condition.accept(this);
        println(") {");

        indentLevel++;
        currentContext = statement.trueBlock.context;
        statement.trueBlock.accept(this);
        indentLevel--;

        if (statement.falseStatement != null) {
            println("} else {");
            indentLevel++;
            statement.falseStatement.accept(this);
            indentLevel--;
        }
        println("}");

        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    @Override
    public Optional<String> visitUse(UseStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitMatch(CaseStatement statement) {
        for (var entry : statement.cases.entrySet()) {
            var typeStmt = entry.getKey();
            var pair = entry.getValue();
            var scopedName = pair.getValue0();
            var block = pair.getValue1();

            indent();
            print("if (");
            statement.expression.accept(this);
            print(" instanceof ");

            var actualType = statement.types.get(typeStmt);
            String javaTypeName = getJavaTypeName(actualType);

            String tempVarName = "temp_" + scopedName;
            if (actualType instanceof BuiltInType builtIn) {
                switch (builtIn) {
                    case INT -> javaTypeName = "Integer";
                    case FLOAT -> javaTypeName = "Float";
                    case DOUBLE -> javaTypeName = "Double";
                    case BOOLEAN -> javaTypeName = "Boolean";
                    default -> {}
                }
            }

            print(javaTypeName + " " + tempVarName);
            println(") {");

            indentLevel++;

            if (actualType instanceof BuiltInType builtIn && builtIn != BuiltInType.STRING) {
                indent();
                print(getJavaTypeName(actualType) + " " + scopedName + " = ");
                print(tempVarName);

                switch (builtIn) {
                    case INT -> print(".intValue()");
                    case FLOAT -> print(".floatValue()");
                    case DOUBLE -> print(".doubleValue()");
                    case BOOLEAN -> print(".booleanValue()");
                    default -> {}
                }
                println(";");
            } else {
                indent();
                print(getJavaTypeName(actualType) + " " + scopedName + " = ");
                print("(" + javaTypeName + ") " + tempVarName);
                println(";");
            }

            currentContext = block.context;
            block.accept(this);
            indentLevel--;

            println("}");
        }

        return Optional.empty();
    }

    @Override
    public Optional<String> visitParameterStmt(ParameterStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitReturnStmt(ReturnStatement statement) {
        indent();
        print("return");
        if (!(statement.expression instanceof EmptyExpression)) {
            print(" ");
            statement.expression.accept(this);
        }
        println(";");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitStruct(StructStatement statement) {
        var structType = currentContext.getVariableTyped(statement.name.source(), StructType.class);

        println("public static class " + structType.name + " {");

        indentLevel++;
        for (var pair : structType.parameters) {
            String fieldName = pair.getValue0();
            IxType fieldType = pair.getValue1();
            String javaType = getJavaTypeName(fieldType);
            println("public " + javaType + " " + fieldName + ";");
        }

        println("");
        print("public " + structType.name + "(");

        for (int i = 0; i < structType.parameters.size(); i++) {
            var param = structType.parameters.get(i);
            if (i > 0) print(", ");
            String javaType = getJavaTypeName(param.getValue1());
            print(javaType + " " + param.getValue0());
        }
        println(") {");

        indentLevel++;
        for (var param : structType.parameters) {
            println("this." + param.getValue0() + " = " + param.getValue0() + ";");
        }
        indentLevel--;
        println("}");

        println("");
        println("@Override");
        println("public String toString() {");
        indentLevel++;
        print("return \"" + structType.name + "{\" + ");
        for (int i = 0; i < structType.parameters.size(); i++) {
            var param = structType.parameters.get(i);
            String fieldName = param.getValue0();
            if (i > 0) print(" + \", \" + ");
            print("\"" + fieldName + "=\" + " + fieldName);
        }
        println(" + \"}\";");
        indentLevel--;
        println("}");
        indentLevel--;
        println("}");

        return Optional.empty();
    }

    @Override
    public Optional<String> visitTypeAlias(TypeStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitUnionType(UnionTypeStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitVariable(VariableStatement statement) {
        indent();
        var type = currentContext.getVariable(statement.identifier());
        String javaType = getJavaTypeName(type);
        print(javaType + " " + statement.identifier() + " = ");
        statement.expression.accept(this);
        println(";");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitWhile(WhileStatement statement) {
        indent();
        print("while (");
        statement.condition.accept(this);
        println(") {");

        indentLevel++;
        currentContext = statement.block.context;
        statement.block.accept(this);
        indentLevel--;

        println("}");
        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    private String getJavaTypeName(IxType type) {
        if (type instanceof BuiltInType builtIn) {
            return switch (builtIn) {
                case INT -> "int";
                case FLOAT -> "float";
                case DOUBLE -> "double";
                case BOOLEAN -> "boolean";
                case STRING -> "String";
                case VOID -> "void";
                case ANY -> "Object";
            };
        } else if (type instanceof ListType(IxType contentType)) {
            String elementType = getWrapperTypeName(contentType);
            return "java.util.List<" + elementType + ">";
        } else if (type instanceof UnionType) {
            return "Object";
        } else if (type instanceof StructType structType) {
            return structType.name;
        }
        return type.getName();
    }

    private String getDefaultValue(IxType type) {
        if (type instanceof BuiltInType builtIn) {
            return switch (builtIn) {
                case INT -> "0";
                case FLOAT -> "0.0f";
                case DOUBLE -> "0.0";
                case BOOLEAN -> "false";
                case STRING, ANY -> "null";
                case VOID -> "";
            };
        } else if (type instanceof ListType) {
            return "null";
        } else if (type instanceof UnionType) {
            return "null";
        } else if (type instanceof StructType) {
            return "null";
        }
        return "null";
    }

    public Map<String, String> getStructClasses() {
        return structClasses;
    }


}