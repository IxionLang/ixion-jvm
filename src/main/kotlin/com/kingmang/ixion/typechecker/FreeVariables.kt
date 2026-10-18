package com.kingmang.ixion.typechecker

import com.kingmang.ixion.ast.*

/** Lexical free-name analysis; patterns and declarations introduce bindings in their own scope. */
object FreeVariables {
    fun of(expression: LambdaExpression): Set<String> {
        val free = linkedSetOf<String>()
        fun walkExpr(node: Expression, bound: Set<String>, walkBlock: (BlockStatement, Set<String>) -> Unit) {
            fun walk(value: Expression) = walkExpr(value, bound, walkBlock)
            when (node) {
                is IdentifierExpression -> if (node.identifier.source !in bound) free.add(node.identifier.source)
                is AssignExpression -> { walk(node.left); walk(node.right) }
                is BinaryExpression -> { walk(node.left); walk(node.right) }
                is CallExpression -> { walk(node.item); node.arguments.forEach(::walk) }
                is GroupingExpression -> walk(node.expression)
                is IndexAccessExpression -> { walk(node.left); walk(node.right) }
                is LiteralListExpression -> node.entries.forEach(::walk)
                is TupleExpression -> node.elements.forEach(::walk)
                is PrefixExpression -> walk(node.right)
                is PostfixExpression -> walk(node.expression)
                is PropertyAccessExpression -> walk(node.expression)
                is UpdateExpression -> { walk(node.base); node.fields.forEach { walk(it.second) } }
                is LambdaExpression -> walkBlock(node.body, bound + node.parameters.map { it.name.source })
                is CaseExpression -> {
                    walk(node.subject)
                    node.branches.forEach { branch ->
                        val scope = bound + branch.pattern.bindings().map { it.source }
                        branch.guard?.let { walkExpr(it, scope, walkBlock) }
                        walkBlock(branch.body.block, scope)
                        branch.body.result?.let { walkExpr(it, scope + branch.body.block.statements.filterIsInstance<VariableStatement>().map { it.name.source }, walkBlock) }
                    }
                }
                else -> Unit
            }
        }
        lateinit var block: (BlockStatement, Set<String>) -> Unit
        lateinit var statement: (Statement, MutableSet<String>) -> Unit
        statement = { node, bound ->
            fun expr(value: Expression) = walkExpr(value, bound, block)
            when (node) {
                is VariableStatement -> { expr(node.expression); bound.add(node.name.source) }
                is ReturnStatement -> node.expression?.let(::expr)
                is ExpressionStatement -> expr(node.expression)
                is BlockStatement -> block(node, bound)
                is IfStatement -> { expr(node.condition); block(node.trueBlock, bound); node.falseStatement?.let { statement(it, bound.toMutableSet()) } }
                is WhileStatement -> { expr(node.condition); block(node.block, bound) }
                is ForStatement -> { expr(node.expression); block(node.block, bound + node.name.source) }
                else -> Unit
            }
        }
        block = { node, names ->
            val bound = names.toMutableSet()
            node.statements.filterNotNull().forEach { statement(it, bound) }
        }
        block(expression.body, expression.parameters.map { it.name.source }.toSet())
        return free
    }
}
