package io.microconfig.core.properties.io.properties.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.isDirective
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.impl.AbstractConfigReader
import io.microconfig.core.properties.io.properties.impl.PropertiesSyntax.endsWithContinuation
import io.microconfig.io.FsReader
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import java.io.File

class PropertiesReader(file: File, reader: FsReader) : AbstractConfigReader(file, reader) {
    private val logicalLines = readLogicalLines()

    override fun properties(configType: String, environment: String): List<Property> =
        logicalLines.filterNot { it.directive }.map { toProperty(it, configType, environment) }

    private fun readLogicalLines(): List<LogicalLine> {
        val result = mutableListOf<LogicalLine>()
        var current: StringBuilder? = null
        var firstLine = 0
        lines.forEachIndexed { index, line ->
            if (current == null) {
                if (line.all { PropertiesSyntax.isSkippedBeforeValue(it) }) return@forEachIndexed
                val trimmed = line.trimStart { PropertiesSyntax.isSkippedBeforeValue(it) }
                if (isDirective(trimmed) || trimmed.startsWith('!')) {
                    result.add(LogicalLine(index, line.trim { it <= ' ' }, true))
                    return@forEachIndexed
                }
                current = StringBuilder()
                firstLine = index
            } else if (line.all { PropertiesSyntax.isSkippedBeforeValue(it) }) {
                current.setLength(current.length - LINES_SEPARATOR.length - 1)
                result.add(LogicalLine(firstLine, current.toString(), false))
                current = null
                return@forEachIndexed
            }
            val continued = endsWithContinuation(line)
            val segment = line.trimStart { PropertiesSyntax.isSkippedBeforeValue(it) }
            val atEnd = index == lines.lastIndex
            current.append(if (continued && atEnd) segment.dropLast(1) else segment)
            if (continued && !atEnd) current.append(LINES_SEPARATOR)
            else {
                result.add(LogicalLine(firstLine, current.toString(), false))
                current = null
            }
        }
        return result
    }

    private fun toProperty(line: LogicalLine, configType: String, environment: String): Property {
        val source = fileSource(file, line.lineNumber, false, configType, environment)
        val separator = separatorIn(line.text)
        require(separator >= 0) {
            "Incorrect delimiter in '${line.text}' in '$source'\nProperty must contain ':' or '=' as delimiter."
        }
        return property(
            propertyKey(line.text.substring(0, separator)),
            line.text.substring(separator + 1).trimStart {
                PropertiesSyntax.isSkippedBeforeValue(it)
            },
            PROPERTIES,
            source,
            ValueFormat.PROPERTIES,
        )
    }

    private fun separatorIn(text: String): Int {
        var escaped = false
        text.forEachIndexed { index, character ->
            if (!escaped && (character == ':' || character == '=')) return index
            escaped = !escaped && character == '\\'
        }
        return -1
    }

    private fun propertyKey(text: String): String {
        var end = text.length
        while (
            end > 0 &&
            PropertiesSyntax.isSkippedBeforeValue(text[end - 1]) &&
            !endsWithContinuation(text.substring(0, end - 1))
        ) end--
        return text.substring(0, end).trimStart { PropertiesSyntax.isSkippedBeforeValue(it) }
    }

    override fun format(): ConfigFormat = PROPERTIES

    override fun commentsByLineNumber(): Map<Int, String> =
        logicalLines.filter { it.directive }.associate { it.lineNumber to it.text }.toSortedMap()

    private data class LogicalLine(val lineNumber: Int, val text: String, val directive: Boolean)
}
