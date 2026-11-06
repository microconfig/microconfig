package io.microconfig.core.properties.io.properties.impl

internal object PropertiesSyntax {
    fun endsWithContinuation(text: CharSequence): Boolean =
        text.takeLastWhile { it == '\\' }.length % 2 == 1

    fun isSkippedBeforeValue(character: Char): Boolean =
        character == ' ' || character == '\t' || character == '\u000C'

    fun lineBreakEnd(text: CharSequence, index: Int): Int =
        when (text[index]) {
            '\r' -> if (index + 1 < text.length && text[index + 1] == '\n') index + 1 else index
            '\n' -> index
            else -> -1
        }
}
