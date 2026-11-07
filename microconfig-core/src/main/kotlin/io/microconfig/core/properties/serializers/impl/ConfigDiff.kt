package io.microconfig.core.properties.serializers.impl

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.io.selector.ConfigIoFactory.newConfigIo
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.serializers.ConfigChangeListener
import io.microconfig.io.FsReader
import io.microconfig.utils.Logger.error
import io.microconfig.utils.Logger.warn
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Optional
import java.util.function.Predicate

class ConfigDiff : ConfigChangeListener {
    fun storeDiffFor(previousConfigFile: File, newProperties: Collection<Property>) =
        accept(previousConfigFile, newProperties)

    override fun prepare(
        file: File,
        properties: Collection<Property>,
    ): () -> Unit {
        val previous = readOldConfig(file)
        val changes =
            compare(
                previous.toMutableMap(),
                properties,
                file,
                file.isFile,
            )
        return { storeDiff(file, changes) }
    }

    private fun readOldConfig(file: File): Map<String, String> =
        try {
            configIo().readFrom(file).propertiesAsMap()
        } catch (failure: RuntimeException) {
            error("Can't read previous config '$file' for comparison: ${failure.message}")
            emptyMap()
        }

    // Compare the serialized forms because the old values have been read back from disk.
    private fun compare(
        previous: MutableMap<String, String>,
        current: Collection<Property>,
        file: File,
        hadPrevious: Boolean,
    ): Map<String, String> {
        if (!hadPrevious) return emptyMap()
        val reader = readSerializedConfig(file, current)
        val yaml = reader.format() == YAML
        val changes = sortedMapOf<String, String>()
        reader.propertiesAsMap().forEach { (key, newValue) ->
            val oldValue = previous.remove(key)
            if (oldValue == null) changes["+$key"] = newValue
            else if (newValue != oldValue) changes[key] = changedValue(oldValue, newValue, yaml)
        }
        previous.forEach { (key, value) -> changes["-$key"] = value }
        return changes
    }

    private fun readSerializedConfig(file: File, properties: Collection<Property>): ConfigReader {
        val text = configIo().writeTo(file).serialize(properties)
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

    private fun changedValue(previous: String, current: String, yaml: Boolean): String {
        val message = "$previous -> $current"
        return if (yaml) YamlScalars.stringValue(message) else message
    }

    private fun storeDiff(file: File, changes: Map<String, String>) {
        val diff = File(file.parentFile, "diff-${file.name}")
        if (changes.isEmpty()) deleteDiff(diff)
        else {
            warn("Stored ${changes.size} property changes to ${diff.parentFile.name}/${diff.name}")
            configIo().writeTo(diff).write(changes)
        }
    }

    private fun deleteDiff(file: File) {
        try {
            Files.deleteIfExists(file.toPath())
        } catch (error: IOException) {
            throw IllegalStateException("Can't remove stale diff '$file'", error)
        }
    }
}
