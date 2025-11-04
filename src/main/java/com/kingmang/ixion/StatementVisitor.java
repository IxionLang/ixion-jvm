package com.kingmang.ixion;

import com.kingmang.ixion.ast.*;

public interface StatementVisitor<R> {
    R visit(Statement statement);
    R visitTypeAlias(TypeAliasStatement statement);
    R visitBlockStmt(BlockStatement statement);
    R visitEnum(EnumStatement statement);
    R visitExport(ExportStatement statement);
    R visitExpressionStmt(ExpressionStatement statement);
    R visitFor(ForStatement statement);
    R visitFunctionStmt(DefStatement statement);
    R visitIf(IfStatement statement);
    R visitUse(UseStatement statement);
    R visitMatch(CaseStatement statement);
    R visitParameterStmt(ParameterStatement statement);
    R visitReturnStmt(ReturnStatement statement);
    R visitStruct(StructStatement statement);
    R visitTypeAlias(TypeStatement statement);
    R visitUnionType(UnionTypeStatement statement);
    R visitVariable(VariableStatement statement);
    R visitWhile(WhileStatement statement);
}