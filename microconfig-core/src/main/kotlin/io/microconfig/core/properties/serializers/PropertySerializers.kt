package io.microconfig.core.properties.serializers

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.name
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.serializers.impl.ConfigDiff
import io.microconfig.core.templates.Template
import io.microconfig.utils.FileUtils.copyPermissions
import io.microconfig.utils.FileUtils.write
import io.microconfig.utils.Logger.info
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

object PropertySerializers {
    fun withConfigDiff(): BiConsumer<File, Collection<Property>> =
        BiConsumer(ConfigDiff()::storeDiffFor)

    fun toFileIn(
        dir: File,
        listener: BiConsumer<File, Collection<Property>>,
    ): PropertySerializer<File> {
        val templateOutputs = ConcurrentHashMap.newKeySet<Path>()
        return PropertySerializer { properties, templates, type, component, _ ->
            templateOutputs.addAll(templates.map { outputPath(it.destination) })
            val visible = properties.filterNot { it.isVar }
            val result = resultFile(dir, component, type, properties, visible.isEmpty())
            validateOutput(result, visible, templateOutputs)
            if (outputPath(result) !in templateOutputs) listener.accept(result, properties)
            templates.forEach { copyTemplate(it, component) }
            writeConfig(result, component, visible)
            deleteObsoleteConfigs(result, visible.isEmpty(), templateOutputs)
            result
        }
    }

    private fun outputPath(file: File): Path = file.toPath().toAbsolutePath().normalize()

    private fun resultFile(
        dir: File,
        component: String,
        type: ConfigType,
        properties: Collection<Property>,
        empty: Boolean,
    ): File {
        val preferred =
            File(
                dir,
                "$component/${type.resultFileName}${extensionByConfigFormat(properties).extension()}",
            )
        if (!empty || preferred.isFile) return preferred
        val previous = oppositeConfig(preferred)
        return if (previous.isFile) previous else preferred
    }

    private fun extensionByConfigFormat(properties: Collection<Property>): ConfigFormat =
        if (properties.isEmpty() || properties.any { it.configFormat == YAML }) YAML else PROPERTIES

    private fun oppositeConfig(file: File): File =
        File(
            file.parentFile,
            "${file.nameWithoutExtension}.${if (file.extension == "yaml") "properties" else "yaml"}",
        )

    private fun validateOutput(
        result: File,
        properties: Collection<Property>,
        templates: Set<Path>,
    ) {
        require(properties.isEmpty() || outputPath(result) !in templates) {
            "Config and template have the same destination '$result'"
        }
    }

    private fun copyTemplate(template: Template, component: String) {
        write(template.destination.toPath(), template.contentAsBytes)
        copyPermissions(template.source.toPath(), template.destination.toPath())
        info(
            "Copied '$component' template ../${template.source.parentFile.name}/${template.source.name} -> ${template.fileName}"
        )
    }

    private fun writeConfig(file: File, component: String, properties: Collection<Property>) {
        if (properties.isEmpty()) return
        configIo().writeTo(file).write(properties)
        info("Generated $component/${file.name}")
    }

    private fun deleteObsoleteConfigs(result: File, empty: Boolean, templates: Set<Path>) {
        if (empty) deleteConfig(result, templates)
        val obsolete = oppositeConfig(result)
        deleteConfig(obsolete, templates)
        deleteConfig(File(result.parentFile, "diff-${obsolete.name}"), templates)
    }

    private fun deleteConfig(file: File, templates: Set<Path>) {
        if (outputPath(file) in templates) return
        try {
            Files.deleteIfExists(file.toPath())
        } catch (error: IOException) {
            throw IllegalStateException("Can't remove obsolete config '$file'", error)
        }
    }

    fun asString(): PropertySerializer<String> = PropertySerializer { properties, _, _, _, _ ->
        configIo()
            .writeTo(File(extensionByConfigFormat(properties).extension()))
            .serialize(properties)
    }
}
