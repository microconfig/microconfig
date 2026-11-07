package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.io.properties.PropertiesText
import org.yaml.snakeyaml.DumperOptions.ScalarStyle

internal object YamlPropertyPaths {
    fun fromProperties(key: String): String =
        propertiesParts(PropertiesText.unescape(key)).joinToString(".", transform = ::stringKey)

    private fun stringKey(part: String): String {
        val style =
            if (part.contains('\'') || part.contains('"')) ScalarStyle.DOUBLE_QUOTED
            else ScalarStyle.PLAIN
        return SnakeYaml.scalar(SnakeYaml.text(part, style))
    }

    private fun propertiesParts(key: String): List<String> {
        val parts = mutableListOf<String>()
        var brackets = 0
        var start = 0
        key.forEachIndexed { index, character ->
            when (character) {
                '[' -> brackets++
                ']' -> brackets = (brackets - 1).coerceAtLeast(0)
                '.' ->
                    if (brackets == 0) {
                        parts.add(key.substring(start, index))
                        start = index + 1
                    }
            }
        }
        parts.add(key.substring(start))
        return parts
    }

    fun parts(key: String): List<String> {
        val parts = mutableListOf<String>()
        var brackets = 0
        var quote: Char? = null
        var tag = false
        var start = 0
        var contentStart = skipWhitespace(key, start)
        var index = 0
        while (index < key.length) {
            val character = key[index]
            if (quote != null) {
                if (quote == '"' && character == '\\') index++
                else if (character == quote) {
                    if (quote == '\'' && index + 1 < key.length && key[index + 1] == '\'') index++
                    else quote = null
                }
            } else if (tag) {
                if (character.isWhitespace()) {
                    tag = false
                    contentStart = skipWhitespace(key, index)
                }
            } else
                when (character) {
                    '!' ->
                        if (index == contentStart) {
                            tag = true
                        }
                    '"',
                    '\'' -> if (index == contentStart) quote = character
                    '[' -> brackets++
                    ']' -> brackets = (brackets - 1).coerceAtLeast(0)
                    '.' ->
                        if (brackets == 0) {
                            parts.add(key.substring(start, index))
                            start = index + 1
                            contentStart = skipWhitespace(key, start)
                        }
                }
            index++
        }
        parts.add(key.substring(start))
        return parts
    }

    private fun skipWhitespace(key: String, start: Int): Int {
        var index = start
        while (index < key.length && key[index].isWhitespace()) index++
        return index
    }
}
