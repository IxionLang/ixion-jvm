package com.kingmang.ixion.matching

import com.kingmang.ixion.runtime.IxType

/** Maranget-style usefulness on a pattern matrix, including products and recursive lists. */
class PatternCoverage {
    private var steps = 0
    fun witness(
        rows: List<List<CoverPattern>>,
        vector: List<CoverPattern>,
        types: List<IxType>,
        depth: Int = 0
    ): List<String>? {
        if (++steps > 20000 || depth > 256) throw IllegalStateException("match.analysis_limit: add a covering branch")
        if (vector.isEmpty()) return if (rows.isEmpty()) emptyList() else null
        if (rows.any { row -> row.all { it.constructor == null } }) return null
        val head = vector.first()
        val tail = vector.drop(1)
        val remaining = types.drop(1)
        val ctor = head.constructor
        fun specialize(c: CoverConstructor): List<List<CoverPattern>> = rows.mapNotNull { row ->
            val first = row.first()
            when {
                first.constructor == null -> List(c.fields.size) { CoverPattern() } + row.drop(1)
                first.constructor.key == c.key -> first.fields + row.drop(1)
                else -> null
            }
        }

        fun descend(c: CoverConstructor, fields: List<CoverPattern>): List<String>? {
            val result = witness(specialize(c), fields + tail, c.fields + remaining, depth + 1) ?: return null
            val args = result.take(c.fields.size)
            val rendered = if (args.isEmpty()) c.label else c.label + args.joinToString(", ", "(", ")")
            return listOf(rendered) + result.drop(c.fields.size)
        }
        if (ctor != null) return descend(ctor, head.fields)
        val universe = PatternChecker.universe(types.first())
        val present = rows.mapNotNull { it.first().constructor?.key }.toSet()
        if (universe != null && universe.all { it.key in present }) {
            for (candidate in universe) descend(
                candidate,
                List(candidate.fields.size) { CoverPattern() })?.let { return it }
            return null
        }
        val defaults = rows.filter { it.first().constructor == null }.map { it.drop(1) }
        val result = witness(defaults, tail, remaining, depth + 1) ?: return null
        val missing = universe?.firstOrNull { it.key !in present }
        val sample =
            missing?.let { it.label + if (it.fields.isEmpty()) "" else it.fields.joinToString(", ", "(", ")") { "_" } }
                ?: "_"
        return listOf(sample) + result
    }
}
