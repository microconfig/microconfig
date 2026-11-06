package io.microconfig.core.properties.resolvers.expression.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.ResolveException
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.resolvers.RecursiveResolver
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
        expressionsIn(line).findFirst().map { Expression(it.group(1), it.start(), it.end()) }

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
                val expression =
                    if (PropertiesText.isPropertiesText(sourceOfValue))
                        PropertiesText.unescape(value)
                    else value
                val result = evaluator.evaluate(expression)
                return if (PropertiesText.isPropertiesText(sourceOfValue))
                    PropertiesText.escape(result)
                else result
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
                val end = scanExpression(text, position, result)
                position = text.indexOf("#{", end)
            }
            return result
        }

        private fun scanExpression(
            text: String,
            start: Int,
            expressions: MutableList<MatchResult>,
        ): Int {
            val starts = mutableMapOf(1 to start)
            var depth = 1
            var quote: Char? = null
            var index = start + 2
            while (index < text.length) {
                val character = text[index]
                if (quote != null) {
                    if (character == '\\' && quote == '"') index++
                    else if (character == quote) {
                        if (index + 1 < text.length && text[index + 1] == quote) index++
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
                                addExpression(text, it, index + 1, expressions)
                            }
                            if (--depth == 0) return index + 1
                        }
                    }
                index++
            }
            return text.length
        }

        private fun addExpression(
            text: String,
            start: Int,
            end: Int,
            expressions: MutableList<MatchResult>,
        ) {
            val matcher = expressionPattern.matcher(text).region(start, end)
            if (matcher.matches()) expressions.add(matcher.toMatchResult())
        }
    }
}
