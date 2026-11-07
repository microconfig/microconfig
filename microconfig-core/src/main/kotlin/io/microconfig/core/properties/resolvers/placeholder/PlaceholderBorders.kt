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
        ): Optional<PlaceholderBorders> = Optional.ofNullable(find(line.toString(), startIndex))

        private fun find(text: String, start: Int): PlaceholderBorders? {
            headerPattern.findAll(text, start.coerceAtLeast(0)).forEach { header ->
                val separator = header.groups["separator"]!!.value
                if (separator == "}" && header.groups["key"]!!.value.isEmpty()) return@forEach
                val end =
                    if (separator == "}") header.range.last + 1
                    else defaultValueEnd(text, header.range.last + 1)
                if (end >= 0) return PlaceholderBorders(text, header.range.first, end, header)
            }
            return null
        }

        private fun defaultValueEnd(text: String, start: Int): Int {
            var depth = 1
            var quote: Char? = null
            var lastClosing = -1
            var index = start
            while (index < text.length) {
                val character = text[index]
                if (quote != null) {
                    if (character == '\\' && quote == '"') index++
                    else if (character == quote) {
                        if (index + 1 < text.length && text[index + 1] == quote) index++
                        else quote = null
                    }
                } else
                    when (character) {
                        '\'',
                        '"' -> if (startsQuotedValue(text, index, start)) quote = character
                        '{' -> depth++
                        '}' -> {
                            lastClosing = index
                            if (--depth == 0) return index + 1
                        }
                    }
                index++
            }
            return if (lastClosing < 0) -1 else lastClosing + 1
        }

        private fun startsQuotedValue(text: String, index: Int, start: Int): Boolean =
            (index == start || !text[index - 1].isLetterOrDigit()) &&
                text.indexOf(text[index], index + 1) >= 0
    }
}
