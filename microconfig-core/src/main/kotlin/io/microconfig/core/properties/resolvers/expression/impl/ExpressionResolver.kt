package io.microconfig.core.properties.resolvers.expression.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.ResolveException
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.Optional
import java.util.regex.MatchResult
import java.util.regex.Pattern
import java.util.stream.Stream

class ExpressionResolver : RecursiveResolver {
    private val evaluator =
        ExpressionEvaluator.withFunctionsFrom(
            Class.forName(
                "io.microconfig.core.properties.resolvers.expression.functions.CustomStringApiKt"
            ),
            Class.forName(
                "io.microconfig.core.properties.resolvers.expression.functions.CustomSystemApiKt"
            ),
        )

    override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
        Optional.ofNullable(scanExpressions(line.toString()).firstOrNull()?.let {
            Expression(it.group(1), it.start(), it.end())
        })

    private inner class Expression(
        private val value: String,
        private val start: Int,
        private val end: Int,
    ) : RecursiveResolver.Statement {
        override val startIndex: Int
            get() = start

        override val endIndex: Int
            get() = end

        override fun resolveFor(
            sourceOfValue: DeclaringComponent?,
            root: DeclaringComponent?,
        ): String {
            try {
                return evaluator.evaluate(value)
            } catch (failure: RuntimeException) {
                throw ResolveException(sourceOfValue, root, "Can't evaluate $this", failure)
            }
        }

        override fun toString(): String = "#{$value}"
    }

    companion object {
        private val expressionPattern = Pattern.compile("#\\{(.*)}", Pattern.DOTALL)

        fun expressionsIn(line: CharSequence): Stream<MatchResult> =
            scanExpressions(line.toString()).stream()

        private fun scanExpressions(text: String): List<MatchResult> {
            val result = mutableListOf<MatchResult>()
            var position = text.indexOf("#{")
            while (position >= 0) {
                val end = scanExpression(text, position, text.length, result).first
                position = if (end < 0) -1 else text.indexOf("#{", end)
            }
            return result
        }

        internal fun expressionEnd(text: CharSequence, start: Int): Int =
            scanExpression(text, start, text.length).first

        internal fun expressionQuoteAt(text: CharSequence, offset: Int): Char? =
            scanExpression(text, 0, offset).second

        private fun scanExpression(
            text: CharSequence,
            start: Int,
            limit: Int,
            expressions: MutableList<MatchResult>? = null,
        ): Pair<Int, Char?> {
            val starts = mutableMapOf(1 to start)
            var depth = 1
            var quote: Char? = null
            var index = start + 2
            while (index < limit) {
                val character = text[index]
                if (character == '$' && PlaceholderBorders.hasPlaceholderHeaderAt(text, index)) {
                    val end = PlaceholderBorders.endOfPlaceholder(text, index)
                    if (end >= 0) {
                        index = end
                        continue
                    }
                }
                if (quote != null) {
                    if (character == quote) {
                        if (index + 1 < limit && text[index + 1] == quote) index++
                        else quote = null
                    }
                } else
                    when {
                        character == '\'' || character == '"' -> quote = character
                        character == '#' && index + 1 < text.length && text[index + 1] == '{' -> {
                            starts[++depth] = index
                            index++
                        }
                        character == '{' -> depth++
                        character == '}' -> {
                            starts.remove(depth)?.let {
                                if (expressions != null) addExpression(text, it, index + 1, expressions)
                            }
                            if (--depth == 0) return index + 1 to null
                        }
                    }
                index++
            }
            return -1 to quote
        }

        private fun addExpression(
            text: CharSequence,
            start: Int,
            end: Int,
            expressions: MutableList<MatchResult>,
        ) {
            val matcher = expressionPattern.matcher(text).region(start, end)
            if (matcher.matches()) expressions.add(matcher.toMatchResult())
        }
    }
}
