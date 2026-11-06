package io.microconfig.core.properties.io.properties.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.utils.FileUtils
import java.io.File

class PropertiesWriter(private val file: File) : ConfigWriter {
    override fun write(properties: Map<String, String>) =
        FileUtils.write(file, serializeMap(properties))

    private fun serializeMap(properties: Map<String, String>): String =
        properties.entries.joinToString(FileUtils.LINES_SEPARATOR) { (key, value) ->
            "$key=${escape(value)}"
        }

    override fun write(properties: Collection<Property>) =
        FileUtils.write(file, serialize(properties))

    override fun serialize(properties: Collection<Property>): String =
        properties
            .filterNot { it.isVar }
            .joinToString(FileUtils.LINES_SEPARATOR) { "${it.key}=${serializedValue(it)}" }

    override fun serializedValue(property: Property): String = escape(property.value)

    private fun escape(value: String): String {
        val output = escapeLineBreaks(value)
        return if (output.isNotEmpty() && PropertiesSyntax.isSkippedBeforeValue(output[0]))
            "\\$output"
        else output
    }

    private fun escapeLineBreaks(value: String): String {
        val output = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val end = PropertiesSyntax.lineBreakEnd(value, index)
            if (end < 0) output.append(value[index])
            else {
                output.append(
                    if (PropertiesSyntax.endsWithContinuation(output))
                        value.substring(index, end + 1)
                    else escapedLineBreak(value, index, end)
                )
                index = end
            }
            index++
        }
        return output.toString()
    }

    private fun escapedLineBreak(value: String, start: Int, end: Int): String =
        if (start == end && value[start] == '\r') "\\r" else "\\n"
}
