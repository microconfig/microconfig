package io.microconfig.core.properties.repository

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.isTempProperty
import io.microconfig.core.properties.PropertyImpl.Companion.parse
import io.microconfig.core.properties.io.ConfigIo
import java.io.File

class ConfigFile(val file: File, val configType: String, val environment: String) {
    fun parseUsing(configIo: ConfigIo): RawConfig {
        val reader = configIo.readFrom(file)
        val comments = reader.commentsByLineNumber()
        val includes = parseIncludes(comments.values)
        val properties = reader.properties(configType, environment)
        val temporaryProperties = parseTempProperties(comments, reader.format() == YAML)
        return RawConfig(includes, properties + temporaryProperties)
    }

    private fun parseIncludes(comments: Collection<String>): List<Include> =
        comments.filter(Includes::isInclude).flatMap {
            Includes.from(it).withDefaultEnv(environment)
        }

    private fun parseTempProperties(comments: Map<Int, String>, yamlFile: Boolean): List<Property> =
        comments.filterValues(::isTempProperty).map { (line, value) ->
            parse(value, PROPERTIES, fileSource(file, line, yamlFile, configType, environment))
        }

    fun withEnvironment(environment: String): ConfigFile =
        if (this.environment == environment) this else ConfigFile(file, configType, environment)

    override fun equals(other: Any?): Boolean = other is ConfigFile && file == other.file

    override fun hashCode(): Int = file.hashCode()

    override fun toString(): String = file.toString()
}
