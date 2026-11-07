package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.io.yaml.ExpressionValueCache
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.regex.MatchResult

/** Keeps the identity of independent expressions while placeholders change scalar offsets. */
internal class ScalarExpressions(
    original: String,
    private val scalarStart: Int,
    private val cache: ExpressionValueCache,
) {
    private val expressions = linkedMapOf<String, Pair<Int, String>>()
    val value: String = markIndependentExpressions(original)

    private fun markIndependentExpressions(original: String): String {
        if ("#{" !in original) return original
        val statements = independentExpressions(original)
        if (statements.isEmpty()) return original
        val prefix = markerPrefix(original)
        val result = StringBuilder(original)
        statements.asReversed().forEach { statement ->
            val marker = "$prefix${statement.start()}}"
            expressions[marker] = statement.start() to statement.group()
            result.replace(statement.start(), statement.end(), marker)
        }
        return result.toString()
    }

    private fun independentExpressions(value: String): List<MatchResult> {
        val placeholders = placeholderRanges(value)
        val candidates = ExpressionResolver.expressionsIn(value).toList()
            .filter { expression ->
                placeholders.none { it.first < expression.end() && expression.start() <= it.last }
            }
            .sortedWith(compareBy<MatchResult> { it.start() }.thenByDescending { it.end() })
        val selected = mutableListOf<MatchResult>()
        candidates.forEach { expression ->
            if (selected.isEmpty() || expression.start() >= selected.last().end()) selected.add(expression)
        }
        return selected
    }

    private fun placeholderRanges(value: String): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var start = value.indexOf("\${")
        while (start >= 0) {
            val end = PlaceholderBorders.endOfPlaceholder(value, start)
            if (end > start) ranges.add(start until end)
            start = value.indexOf("\${", if (end > start) end else start + 2)
        }
        return ranges
    }

    private fun markerPrefix(value: String): String {
        var prefix = "#{\uE000"
        while (prefix in value) prefix += '\uE000'
        return prefix
    }

    fun findStatementIn(value: CharSequence, evaluate: (String) -> String): RecursiveResolver.Statement? {
        if (expressions.isEmpty()) return null
        val marker = ExpressionResolver.expressionsIn(value).filter { it.group() in expressions }
            .findFirst().orElse(null) ?: return null
        val (position, expression) = expressions.getValue(marker.group())
        return object : RecursiveResolver.Statement {
            override val startIndex = marker.start()
            override val endIndex = marker.end()

            override fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?): String =
                cache.resolve(scalarStart, position, expression) { evaluate(expression) }
        }
    }
}
