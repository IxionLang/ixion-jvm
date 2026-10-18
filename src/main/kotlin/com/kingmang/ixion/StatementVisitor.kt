package com.kingmang.ixion

import com.kingmang.ixion.ast.*

interface StatementVisitor<R> {
    fun visit(statement: Statement): R
    fun visitTypeAlias(statement: TypeAliasStatement): R
    fun visitBlockStmt(statement: BlockStatement): R
    fun visitEnum(statement: EnumStatement): R
    fun visitExport(statement: ExportStatement): R
    fun visitExpressionStmt(statement: ExpressionStatement): R
    fun visitFor(statement: ForStatement): R
    fun visitFunctionStmt(statement: DefStatement): R
    fun visitIf(statement: IfStatement): R
    fun visitUse(statement: UseStatement): R
    fun visitParameterStmt(statement: ParameterStatement): R
    fun visitReturnStmt(statement: ReturnStatement): R
    fun visitStruct(statement: StructStatement): R
    fun visitTypeAlias(statement: TypeStatement): R
    fun visitUnionType(statement: UnionTypeStatement): R
    fun visitVariable(statement: VariableStatement): R
    fun visitWhile(statement: WhileStatement): R
}