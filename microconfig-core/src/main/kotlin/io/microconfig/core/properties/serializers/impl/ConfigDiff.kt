package io.microconfig.core.properties.serializers.impl

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.serializers.ConfigChangeListener
import io.microconfig.io.FsReader
import io.microconfig.utils.Logger.error
import io.microconfig.utils.Logger.warn
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.function.Predicate
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo as defaultConfigIo

class ConfigDiff(private val configIo: ConfigIo = defaultConfigIo()) : ConfigChangeListener {
    private var sourceReader: FsReader? = null

    fun storeDiffFor(previousConfigFile: File, newProperties: Collection<Property>) =
        accept(previousConfigFile, newProperties)

    override fun prepareAll(outputs: Map<File, Collection<Property>>): Map<File, () -> Unit> {
        val prepared = outputs.entries.parallelStream().map { (file, properties) ->
            runCatching { file to prepare(file, properties) }
        }.toList()
        return prepared.map { it.getOrThrow() }.toMap()
    }

    override fun prepare(
        file: File,
        properties: Collection<Property>,
    ): () -> Unit {
        val changes =
            if (file.isFile) compare(properties, file)
            else emptyMap()
        return { storeDiff(file, changes) }
    }

    // Compare the serialized forms because the old values have been read back from disk.
    private fun compare(
        current: Collection<Property>,
        file: File,
    ): Map<String, String> {
        val text = configIo.writeTo(file).serialize(current)
        val previous = readChangedPreviousConfig(file, text)?.toMutableMap() ?: return emptyMap()
        val reader = readSerializedConfig(file, text)
        val yaml = reader.format() == YAML
        val changes = sortedMapOf<String, String>()
        reader.propertiesAsMap().forEach { (key, newValue) ->
            val oldValue = previous.remove(key)
            if (oldValue == null) changes[changedKey(key, '+', yaml)] = newValue
            else if (newValue != oldValue) changes[changedKey(key, null, yaml)] = changedValue(oldValue, newValue, yaml)
        }
        previous.forEach { (key, value) -> changes[changedKey(key, '-', yaml)] = value }
        return changes
    }

    // null means the complete serialized content is already identical.
    private fun readChangedPreviousConfig(file: File, current: String): Map<String, String>? =
        try {
            val source = sourceReader
            if (source != null && file.extension in setOf("yaml", "yml")) {
                val text = source.readFully(file)
                if (text == current) null else readSerializedConfig(file, text).propertiesAsMap()
            } else configIo.readFrom(file).propertiesAsMap()
        } catch (failure: RuntimeException) {
            error("Can't read previous config '$file' for comparison: ${failure.message}")
            emptyMap()
        }

    private fun readSerializedConfig(file: File, text: String): ConfigReader {
        val contentReader =
            object : FsReader {
                override fun readFully(file: File): String = text

                override fun readLines(file: File): List<String> = text.reader().readLines()

                override fun firstLineOf(
                    file: File,
                    predicate: Predicate<String>,
                ): Optional<String> =
                    Optional.ofNullable(readLines(file).firstOrNull { predicate.test(it) })
            }
        return newConfigIo(contentReader).readFrom(file)
    }

    private fun changedKey(key: String, sign: Char?, yaml: Boolean): String {
        if (yaml) return YamlScalars.diffKeyPath(key, sign)
        if (sign != null) return "$sign$key"
        val first = PropertiesText.unescape(key).firstOrNull()
        return if (first == '+' || first == '-' || first == '\\') "\\\\$key" else key
    }

    private fun changedValue(previous: String, current: String, yaml: Boolean): String {
        val message = "$previous -> $current"
        return if (yaml) YamlScalars.stringValue(message) else message
    }

    private fun storeDiff(file: File, changes: Map<String, String>) {
        val diff = File(file.parentFile, "diff-${file.name}")
        if (changes.isEmpty()) deleteDiff(diff)
        else {
            warn("Stored ${changes.size} property changes to ${diff.parentFile.name}/${diff.name}")
            configIo.writeTo(diff).write(changes)
        }
    }

    private fun deleteDiff(file: File) {
        try {
            Files.deleteIfExists(file.toPath())
        } catch (error: IOException) {
            throw IllegalStateException("Can't remove stale diff '$file'", error)
        }
    }

    companion object {
        internal fun forReader(reader: FsReader, configIo: ConfigIo = newConfigIo(reader)): ConfigDiff =
            ConfigDiff(configIo).also { it.sourceReader = reader }
    }
}
