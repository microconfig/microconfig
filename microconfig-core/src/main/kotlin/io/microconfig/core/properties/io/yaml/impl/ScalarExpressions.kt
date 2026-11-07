package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.io.yaml.ExpressionResultCache
import io.microconfig.core.properties.io.yaml.ExpressionValueCache
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.regex.MatchResult

/** Keeps expression identities while placeholders change scalar offsets and choose defaults. */
internal class ScalarExpressions(
    original: String,
    private val scalarStart: Int,
    private val cache: ExpressionValueCache,
) {
    private val expressions = linkedMapOf<String, MarkedExpression>()
    val value: String = markExpressions(original)

    fun findStatementIn(
        value: CharSequence,
        evaluate: (String, ExpressionResultCache?) -> String,
    ): RecursiveResolver.Statement? {
        val marker = findMarker(value) ?: return null
        return cachedStatement(marker, evaluate)
    }

    fun containsMarkedExpression(value: String): Boolean = expressions.keys.any(value::contains)

    private fun markExpressions(original: String): String {
        if ("#{" !in original) return original
        val ranges = expressionRanges(original)
        if (ranges.isEmpty()) return original
        val prefix = markerPrefix(original)
        ranges.forEach { range ->
            val raw = original.substring(range.first, range.last + 1)
            val children = outermost(ranges.filter { it != range && it.first >= range.first && it.last <= range.last })
            val prepared = replaceExpressions(raw, children, prefix, range.first)
            expressions["$prefix${range.first}}"] = MarkedExpression(range.first, prepared)
        }
        return replaceExpressions(original, outermost(ranges), prefix, 0)
    }

    private fun expressionRanges(value: String): List<IntRange> {
        val ranges = linkedSetOf<IntRange>()
        addExpressions(value, 0, ranges)
        var start = value.indexOf("\${")
        while (start >= 0) {
            val end = PlaceholderBorders.endOfPlaceholder(value, start)
            if (end > start) addExpressions(value.substring(start, end), start, ranges)
            start = value.indexOf("\${", start + 2)
        }
        return ranges.toList()
    }

    private fun addExpressions(value: String, offset: Int, ranges: MutableSet<IntRange>) {
        ExpressionResolver.expressionsIn(value).forEach { ranges.add(offset + it.start() until offset + it.end()) }
    }

    private fun markerPrefix(value: String): String {
        var prefix = "#{\uE000"
        while (prefix in value) prefix += '\uE000'
        return prefix
    }

    private fun outermost(ranges: List<IntRange>): List<IntRange> =
        ranges.filter { range ->
            ranges.none { it != range && it.first <= range.first && it.last >= range.last }
        }.sortedBy { it.first }

    private fun replaceExpressions(value: String, ranges: List<IntRange>, prefix: String, offset: Int): String {
        val result = StringBuilder(value)
        ranges.asReversed().forEach { range ->
            result.replace(range.first - offset, range.last + 1 - offset, "$prefix${range.first}}")
        }
        return result.toString()
    }

    private fun findMarker(value: CharSequence): MatchResult? =
        ExpressionResolver.expressionsIn(value).filter { it.group() in expressions }.findFirst().orElse(null)

    private fun cachedStatement(
        marker: MatchResult,
        evaluate: (String, ExpressionResultCache?) -> String,
    ): RecursiveResolver.Statement {
        val expression = expressions.getValue(marker.group())
        val cacheResult: ExpressionResultCache = { expanded, calculate ->
            cache.resolve(scalarStart, expression.position, expanded, calculate)
        }
        return object : RecursiveResolver.Statement {
            override val startIndex = marker.start()
            override val endIndex = marker.end()

            // Resolve dependencies before caching the final SpEL evaluation.
            override fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?): String =
                evaluate(expression.value, cacheResult)
        }
    }

    private data class MarkedExpression(val position: Int, val value: String)
}
