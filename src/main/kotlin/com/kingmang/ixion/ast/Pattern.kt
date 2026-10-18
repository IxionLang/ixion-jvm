package com.kingmang.ixion.ast

import com.kingmang.ixion.lexer.Position
import com.kingmang.ixion.lexer.Token
import com.kingmang.ixion.parser.Node

/** Syntax is separate from expressions: matching never invokes a user constructor. */
sealed class Pattern(final override val position: Position, var end: Position = position) : Node
class WildcardPattern(pos: Position) : Pattern(pos)
class BindingPattern(val name: Token) : Pattern(Position(name.line, name.col))
class LiteralPattern(pos: Position, val value: Expression) : Pattern(pos)
class ConstructorPattern(pos: Position, val owner: TypeStatement, val variant: Token, val fields: List<Pattern>) : Pattern(pos)
class StructPattern(pos: Position, val type: TypeStatement, val fields: List<Pair<Token, Pattern>>) : Pattern(pos)
class TuplePattern(pos: Position, val elements: List<Pattern>) : Pattern(pos)
class ListPattern(pos: Position, val elements: List<Pattern>, val rest: Pattern?) : Pattern(pos)
class TypePattern(pos: Position, val type: TypeStatement, val binding: Token) : Pattern(pos)

fun Pattern.bindings(): List<Token> = when (this) {
    is BindingPattern -> listOf(name)
    is TypePattern -> listOf(binding)
    is ConstructorPattern -> fields.flatMap { it.bindings() }
    is StructPattern -> fields.flatMap { it.second.bindings() }
    is TuplePattern -> elements.flatMap { it.bindings() }
    is ListPattern -> elements.flatMap { it.bindings() } + rest?.bindings().orEmpty()
    else -> emptyList()
}
