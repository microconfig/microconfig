package io.microconfig.core.properties.io.impl

import io.microconfig.core.properties.PropertyImpl.Companion.isDirective
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.io.FsReader
import java.io.File

abstract class AbstractConfigReader(protected val file: File, protected val lines: List<String>) :
    ConfigReader {
    protected constructor(file: File, reader: FsReader) : this(file, reader.readLines(file))

    override fun propertiesAsMap(): Map<String, String> =
        properties("", "").associate { it.key to it.value }.toSortedMap()

    override fun commentsByLineNumber(): Map<Int, String> {
        val valueLines = literalValueLines()
        return lines
            .mapIndexedNotNull { index, line ->
                val value = line.trim { it <= ' ' }
                if (index !in valueLines && isDirective(value)) index to value else null
            }
            .toMap()
            .toSortedMap()
    }

    protected open fun literalValueLines(): Set<Int> = emptySet()
}
