package com.kingmang.ixion.codegen;

import com.kingmang.ixion.Visitor;
import com.kingmang.ixion.api.Context;
import com.kingmang.ixion.api.IxApi;
import com.kingmang.ixion.api.IxFile;
import com.kingmang.ixion.ast.*;
import com.kingmang.ixion.runtime.DefType;
import com.kingmang.ixion.runtime.IxType;
import com.kingmang.ixion.runtime.StructType;

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

    public JavaCodegenVisitor(IxApi ixApi, IxFile source) {
        this.ixApi = ixApi;
        this.source = source;
        this.currentContext = source.rootContext;
    }


    private void indent() {
        for (int i = 0; i < indentLevel; i++) {
            output.append("    ");
        }
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
    public Optional<String> visitAssignExpr(AssignExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBad(BadExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBinaryExpr(BinaryExpression expr) {
        print("(");
        expr.left.accept(this);
        print(" " + expr.operator.source() + " ");
        expr.right.accept(this);
        print(")");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitCall(CallExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitEmpty(EmptyExpression empty) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitEmptyList(EmptyListExpression emptyList) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitGroupingExpr(GroupingExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitIdentifierExpr(IdentifierExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitIndexAccess(IndexAccessExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitLiteralExpr(LiteralExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitLiteralList(LiteralListExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitModuleAccess(ModuleAccessExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPostfixExpr(PostfixExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPrefix(PrefixExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitPropertyAccess(PropertyAccessExpression expr) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visit(Statement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitTypeAlias(TypeAliasStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitBlockStmt(BlockStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitEnum(EnumStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitExport(ExportStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitExpressionStmt(ExpressionStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitFor(ForStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitFunctionStmt(FunctionStatement statement) {
        var funcType = currentContext.getVariableTyped(statement.name.source(), DefType.class);
        functionStack.push(funcType);

        indent();
        print("public static ");
        if(funcType.name.equals("main")){
            print("void main(String[] args)");
        }else {
            print(funcType.returnType.getName() + " ");
            print("_" + funcType.name + "(");

            for (int i = 0; i < funcType.parameters.size(); i++) {
                var param = funcType.parameters.get(i);
                if (i > 0) print(", ");
                print(param.getValue1().getName() + " " + param.getValue0());
            }
            print(")");
        }
        println(" {");

        indentLevel++;
        currentContext = statement.body.context;
        statement.body.accept(this);
        indentLevel--;

        println("}");
        functionStack.pop();
        currentContext = currentContext.getParent();
        return Optional.empty();
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

        indent();
        if (statement.falseStatement != null) {
            println("} else {");
            indentLevel++;
            statement.falseStatement.accept(this);
            indentLevel--;
        }
        indent();
        println("}");

        currentContext = currentContext.getParent();
        return Optional.empty();
    }

    @Override
    public Optional<String> visitUse(UseStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitMatch(MatchStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitParameterStmt(ParameterStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitReturnStmt(ReturnStatement statement) {
        return Optional.empty();
    }

    @Override
    public Optional<String> visitStruct(StructStatement statement) {
        var structType = currentContext.getVariableTyped(statement.name.source(), StructType.class);

        StringBuilder structCode = new StringBuilder();
        int savedIndent = indentLevel;
        indentLevel = 1;

        for (var pair : structType.parameters) {
            String fieldName = pair.getValue0();
            IxType fieldType = pair.getValue1();
            indent();
            structCode.append("public ").append(fieldType.getName())
                    .append(" ").append(fieldName).append(";\n");
        }

        structCode.append("\n");
        indent();
        structCode.append("public ").append(structType.name).append("(");

        for (int i = 0; i < structType.parameters.size(); i++) {
            var param = structType.parameters.get(i);
            if (i > 0) structCode.append(", ");
            structCode.append(param.getValue1().getName()).append(" ").append(param.getValue0());
        }
        structCode.append(") {\n");

        indentLevel = 2;
        for (var param : structType.parameters) {
            indent();
            structCode.append("this.").append(param.getValue0())
                    .append(" = ").append(param.getValue0()).append(";\n");
        }
        indentLevel = 1;
        indent();
        structCode.append("}\n");

        indentLevel = savedIndent;
        structClasses.put(structType.name, structCode.toString());
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
        print(type.getName() + " " + statement.identifier() + " = ");
        statement.expression.accept(this);
        println(";");
        return Optional.empty();
    }

    @Override
    public Optional<String> visitWhile(WhileStatement statement) {
        return Optional.empty();
    }

    public Map<String, String> getStructClasses() {
        return structClasses;
    }

}