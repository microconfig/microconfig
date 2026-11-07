package io.microconfig.core.properties.io.selector.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.PropertyImpl.Companion.isDirective
import io.microconfig.core.properties.io.selector.ConfigFormatDetector
import io.microconfig.io.FsReader
import java.io.File
import kotlin.jvm.optionals.getOrNull

class ConfigFormatDetectorImpl(private val reader: FsReader) : ConfigFormatDetector {
    override fun detectConfigFormat(file: File): ConfigFormat =
        when (file.extension) {
            "properties" -> PROPERTIES
            "yaml",
            "yml" -> YAML
            else -> detectContent(file)
        }

    private fun detectContent(file: File): ConfigFormat {
        if (!file.exists()) return PROPERTIES
        val first =
            reader
                .firstLineOf(file) {
                    it.isNotBlank() &&
                        !isDirective(it.trim()) &&
                        (!it.trimStart().startsWith('!') || separatorIn(it) >= 0)
                }
                .getOrNull() ?: return PROPERTIES
        if (
            first.trim() == "---" ||
                first.trim().startsWith("%YAML ") ||
                first.trim().startsWith("%TAG ")
        )
            return YAML
        val separator = separatorIn(first)
        require(separator >= 0) { "Incorrect property $first in $file" }
        return if (first[separator] == ':') YAML else PROPERTIES
    }

    private fun separatorIn(text: String): Int {
        val keyStart = text.indexOfFirst { !Character.isWhitespace(it) }
        var quote: Char? = null
        var escaped = false
        var firstSeparator = -1
        var index = 0
        while (index < text.length) {
            val character = text[index]
            if (!escaped) {
                if (character == ':' || character == '=') {
                    if (quote == null) return index
                    if (firstSeparator < 0) firstSeparator = index
                }
                if (character == quote) {
                    if (index + 1 < text.length && text[index + 1] == quote) index++
                    else quote = null
                } else if (
                    quote == null &&
                        (character == '\'' || character == '"') &&
                        (index == keyStart || text[index - 1] == '.')
                )
                    quote = character
            }
            escaped = !escaped && character == '\\' && quote != '\''
            index++
        }
        return firstSeparator
    }
}
