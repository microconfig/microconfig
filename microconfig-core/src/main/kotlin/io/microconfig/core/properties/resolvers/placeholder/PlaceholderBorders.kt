package io.microconfig.core.properties.resolvers.placeholder

import java.util.Optional

class PlaceholderBorders
private constructor(
    private val text: String,
    val startIndex: Int,
    val endIndex: Int,
    private val header: MatchResult,
) {
    val hasExplicitEnvironment: Boolean
        get() = header.groups["environment"] != null

    fun toPlaceholder(contextConfigType: String, contextEnv: String): Placeholder =
        Placeholder(
            header.groups["type"]?.value ?: contextConfigType,
            header.groups["component"]!!.value,
            header.groups["environment"]?.value ?: contextEnv,
            header.groups["key"]!!.value,
            if (header.groups["separator"]!!.value == ":")
                text.substring(header.range.last + 1, endIndex - 1)
            else null,
        )

    override fun toString(): String = text.substring(startIndex, endIndex)

    companion object {
        private const val NAME = "[\\p{L}\\p{Nd}._-]"
        private val headerPattern =
            Regex(
                "\\$\\{(?:(?<type>$NAME+(?:::$NAME+)*)::)?(?<component>$NAME+)(?:\\[(?<environment>$NAME*)])?@(?<key>[\\p{L}\\p{Nd}._*/\\\\-]*)(?<separator>[:}])"
            )

        @JvmOverloads
        fun findPlaceholderIn(
            line: CharSequence,
            startIndex: Int = 0,
        ): Optional<PlaceholderBorders> = Optional.ofNullable(findPlaceholderOrNull(line, startIndex))

        internal fun findPlaceholderOrNull(line: CharSequence, startIndex: Int = 0): PlaceholderBorders? =
            find(line.toString(), startIndex)

        internal fun endOfPlaceholder(text: CharSequence, startIndex: Int): Int =
            if (text.getOrNull(startIndex) != '$' || text.getOrNull(startIndex + 1) != '{') -1
            else defaultValueEnd(text.toString(), startIndex + 2, allowIncomplete = false)

        internal fun hasPlaceholderHeaderAt(text: CharSequence, startIndex: Int): Boolean =
            startsPlaceholderAt(text.toString(), startIndex)

        private fun find(text: String, start: Int): PlaceholderBorders? {
            val first = text.indexOf("\${", start.coerceAtLeast(0))
            if (first < 0) return null
            headerPattern.findAll(text, first).forEach { header ->
                val separator = header.groups["separator"]!!.value
                if (separator == "}" && header.groups["key"]!!.value.isEmpty()) return@forEach
                val end =
                    if (separator == "}") header.range.last + 1
                    else defaultValueEnd(text, header.range.last + 1)
                if (end >= 0) return PlaceholderBorders(text, header.range.first, end, header)
            }
            return null
        }

        private fun defaultValueEnd(text: String, start: Int, allowIncomplete: Boolean = true): Int {
            val scopes = mutableListOf(DefaultScope(expression = false))
            var lastClosing = -1
            var index = start
            while (index < text.length) {
                val character = text[index]
                val scope = scopes.last()
                if (scope.quote != null) {
                    if (character == '$' && startsPlaceholderAt(text, index)) {
                        scopes.add(DefaultScope(expression = false))
                        index++
                    } else if (character == '\\' && scope.quote == '"' && !scope.expression) {
                        index++
                    } else if (character == scope.quote) {
                        if (index + 1 < text.length && text[index + 1] == scope.quote) index++
                        else scope.quote = null
                    }
                } else
                    when (character) {
                        '\'',
                        '"' ->
                            if (scope.expression || startsQuotedValue(text, index, start))
                                scope.quote = character
                        '{' ->
                            scopes.add(
                                DefaultScope(
                                    expression = when (text.getOrNull(index - 1)) {
                                        '#' -> true
                                        '$' -> false
                                        else -> scope.expression
                                    }
                                )
                            )
                        '}' -> {
                            lastClosing = index
                            scopes.removeAt(scopes.lastIndex)
                            if (scopes.isEmpty()) return index + 1
                        }
                    }
                index++
            }
            return if (allowIncomplete && lastClosing >= 0) lastClosing + 1 else -1
        }

        private fun startsPlaceholderAt(text: String, start: Int): Boolean {
            var position = start
            while (text.startsWith("\${", position)) {
                if (headerPattern.matchAt(text, position) != null) return true
                position += 2
                while (position < text.length && isHeaderCharacter(text[position])) position++
            }
            return false
        }

        private fun isHeaderCharacter(character: Char): Boolean =
            character.isLetterOrDigit() || character in "._-:@[]*/\\"

        private fun startsQuotedValue(text: String, index: Int, start: Int): Boolean =
            (index == start || !text[index - 1].isLetterOrDigit()) &&
                text.indexOf(text[index], index + 1) >= 0

        private class DefaultScope(val expression: Boolean, var quote: Char? = null)
    }
}
