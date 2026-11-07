package io.microconfig.core.properties.io.properties

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.FileBasedComponent
import io.microconfig.core.properties.io.properties.impl.PropertiesSyntax

/** Properties escapes are decoded only when text crosses a configuration-format boundary. */
object PropertiesText {
    fun isPropertiesText(component: DeclaringComponent?): Boolean =
        component is FileBasedComponent && !component.isYaml

    fun escape(plain: String): String = escape(plain, normalizeLineBreaks = true)

    internal fun escapeValue(plain: String): String = escape(plain, normalizeLineBreaks = false)

    private fun escape(plain: String, normalizeLineBreaks: Boolean): String {
        val output = StringBuilder(plain.length)
        appendEscaped(plain, output, normalizeLineBreaks)
        return output.toString()
    }

    private fun appendEscaped(plain: String, output: StringBuilder, normalizeLineBreaks: Boolean) {
        var index = 0
        while (index < plain.length) {
            when (val character = plain[index]) {
                '\\' -> output.append("\\\\")
                '\n' -> output.append("\\n")
                '\r' -> {
                    val lineBreak = normalizeLineBreaks && index + 1 < plain.length && plain[index + 1] == '\n'
                    output.append(if (lineBreak) "\\n" else "\\r")
                    if (lineBreak) index++
                }
                else -> output.append(character)
            }
            index++
        }
    }

    fun unescape(text: String): String {
        val output = StringBuilder(text.length)
        appendUnescaped(text, output)
        return output.toString()
    }

    private fun appendUnescaped(text: String, output: StringBuilder) {
        var index = 0
        while (index < text.length) {
            if (text[index] != '\\') output.append(text[index])
            else if (index + 1 < text.length) index = unescapeAt(text, index + 1, output)
            index++
        }
    }

    private fun unescapeAt(text: String, index: Int, output: StringBuilder): Int {
        val lineBreak = PropertiesSyntax.lineBreakEnd(text, index)
        if (lineBreak >= 0) return skipContinuationIndent(text, lineBreak + 1) - 1
        val character = text[index]
        if (character == 'u' && isUnicodeEscape(text, index + 1)) {
            output.append(text.substring(index + 1, index + 5).toInt(16).toChar())
            return index + 4
        }
        output.append(
            when (character) {
                't' -> '\t'
                'n' -> '\n'
                'r' -> '\r'
                'f' -> '\u000C'
                else -> character
            }
        )
        return index
    }

    private fun skipContinuationIndent(text: String, start: Int): Int {
        var index = start
        while (index < text.length && PropertiesSyntax.isSkippedBeforeValue(text[index])) index++
        return index
    }

    private fun isUnicodeEscape(text: String, start: Int): Boolean =
        start + 4 <= text.length &&
            (start until start + 4).all { Character.digit(text[it], 16) >= 0 }
}
