package io.microconfig.core.properties.serializers.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.serializers.ConfigChangeListener
import io.microconfig.core.templates.Template
import io.microconfig.utils.FileUtils.copyPermissions
import io.microconfig.utils.FileUtils.write
import io.microconfig.utils.Logger.info
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.BiConsumer

internal class FilePropertySerializer(
    private val directory: File,
    private val listener: BiConsumer<File, Collection<Property>>,
    private val configIo: ConfigIo,
    private val protectedOutputs: Set<Path> = emptySet(),
    private val notifiedOutputs: MutableSet<Path>? = null,
    private val preparedNotifications: Map<Path, () -> Unit> = emptyMap(),
) : PropertySerializer<File> {
    override fun prepare(properties: List<TypedProperties>): PropertySerializer<File> {
        val outputs = collectOutputs(properties)
        val notifications = prepareNotifications(properties, outputs)
        return FilePropertySerializer(
            directory, listener, configIo, outputs, mutableSetOf(), notifications
        )
    }

    private fun collectOutputs(properties: List<TypedProperties>): Set<Path> {
        val destinations = linkedMapOf<Path, () -> ByteArray>()
        val protected = mutableSetOf<Path>()
        properties.forEach { unit ->
            val file =
                resultFile(
                    unit.declaringComponent.component,
                    unit.configType,
                    unit.properties,
                    false,
                )
            require(!isDiffFileName(file.name)) { "Reserved diff output '$file'" }
            unit.templates.forEach { template ->
                reserveOutput(template.destination, { template.contentAsBytes }, destinations)
                protected.addAll(protectedPaths(template.destination))
            }
            if (unit.properties.any { !it.isVar }) {
                reserveOutput(file, { configContent(file, unit.properties) }, destinations)
                protected.addAll(protectedPaths(file))
            }
        }
        return protected
    }

    private fun prepareNotifications(
        properties: List<TypedProperties>,
        protected: Set<Path>,
    ): Map<Path, () -> Unit> {
        if (listener !is ConfigChangeListener) return emptyMap()
        val notifications = linkedMapOf<Path, () -> Unit>()
        properties.forEach { unit ->
            val empty = unit.properties.none { !it.isVar }
            val file =
                resultFile(unit.declaringComponent.component, unit.configType, unit.properties, empty)
            if (!empty || !isProtected(file, protected))
                notifications.getOrPut(outputPath(file)) { listener.prepare(file, unit.properties) }
        }
        return notifications
    }

    private fun reserveOutput(
        file: File,
        content: () -> ByteArray,
        destinations: MutableMap<Path, () -> ByteArray>,
    ) {
        val path = outputPath(file)
        require(!isDiffFileName(file.name) && !isDiffFileName(path.fileName.toString())) {
            "Reserved diff output '$file'"
        }
        val previous = destinations.putIfAbsent(path, content)
        require(previous == null || previous().contentEquals(content())) {
            "Several outputs have the same destination '$file'"
        }
    }

    private fun isDiffFileName(name: String): Boolean =
        name.startsWith("diff-") && (name.endsWith(".yaml") || name.endsWith(".properties"))

    private fun configContent(file: File, properties: Collection<Property>): ByteArray =
        configIo
            .writeTo(file)
            .serialize(properties.filterNot { it.isVar })
            .toByteArray(Charsets.UTF_8)

    private fun outputPath(file: File): Path =
        try {
            resolveLinks(file.toPath().toAbsolutePath(), mutableSetOf())
        } catch (error: IOException) {
            throw UncheckedIOException("Can't resolve output '$file'", error)
        }

    private fun resolveLinks(path: Path, visiting: MutableSet<Path>): Path {
        var result = path.root
        for (part in path) {
            result = result.resolve(part).normalize()
            if (Files.isSymbolicLink(result)) {
                val link = result
                require(visiting.add(link)) { "Cyclic output symlink '$link'" }
                val target = Files.readSymbolicLink(link)
                result =
                    resolveLinks(
                        if (target.isAbsolute) target else link.parent.resolve(target),
                        visiting,
                    )
                visiting.remove(link)
            } else if (Files.exists(result)) {
                result = result.toRealPath()
            }
        }
        return result
    }

    private fun protectedPaths(file: File): Set<Path> =
        setOf(file.toPath().toAbsolutePath().normalize(), outputPath(file))

    private fun resultFile(
        component: String,
        type: ConfigType,
        properties: Collection<Property>,
        empty: Boolean,
    ): File {
        val format =
            if (properties.isEmpty() || properties.any { it.configFormat == YAML }) YAML
            else PROPERTIES
        val preferred = File(directory, "$component/${type.resultFileName}${format.extension()}")
        if (!empty || preferred.isFile) return preferred
        val previous = oppositeConfig(preferred)
        return if (previous.isFile) previous else preferred
    }

    private fun oppositeConfig(file: File): File =
        File(
            file.parentFile,
            "${file.nameWithoutExtension}.${if (file.extension == "yaml") "properties" else "yaml"}",
        )

    override fun serialize(
        properties: Collection<Property>,
        templates: List<Template>,
        configType: ConfigType,
        componentName: String,
        environment: String,
    ): File {
        val visible = properties.filterNot { it.isVar }
        val result = resultFile(componentName, configType, properties, visible.isEmpty())
        validateDestinations(result, visible, templates)
        val protected = protectedOutputs + templates.flatMap { protectedPaths(it.destination) }
        val notify = prepareNotification(result, properties, visible.isEmpty(), protected)
        templates.forEach { copyTemplate(it, componentName) }
        writeConfig(result, componentName, visible)
        deleteObsoleteConfigs(result, visible.isEmpty(), protected)
        notify()
        return result
    }

    private fun validateDestinations(
        result: File,
        properties: Collection<Property>,
        templates: List<Template>,
    ) {
        require(!isDiffFileName(result.name)) { "Reserved diff output '$result'" }
        val destinations = linkedMapOf<Path, () -> ByteArray>()
        templates.forEach { template ->
            reserveOutput(template.destination, { template.contentAsBytes }, destinations)
        }
        if (properties.isNotEmpty())
            reserveOutput(result, { configContent(result, properties) }, destinations)
    }

    private fun prepareNotification(
        file: File,
        properties: Collection<Property>,
        empty: Boolean,
        protected: Set<Path>,
    ): () -> Unit {
        if (empty && isProtected(file, protected)) return {}
        val path = outputPath(file)
        if (notifiedOutputs?.contains(path) == true) return {}
        val notify = prepareListenerNotification(file, properties)
        return {
            notify()
            notifiedOutputs?.add(path)
        }
    }

    private fun prepareListenerNotification(file: File, properties: Collection<Property>): () -> Unit {
        if (listener is ConfigChangeListener)
            return preparedNotifications[outputPath(file)] ?: listener.prepare(file, properties)
        listener.accept(file, properties)
        return {}
    }

    private fun isProtected(file: File, protected: Set<Path>): Boolean =
        if (Files.isSymbolicLink(file.toPath()))
            file.toPath().toAbsolutePath().normalize() in protected
        else outputPath(file) in protected

    private fun copyTemplate(template: Template, component: String) {
        write(template.destination.toPath(), template.contentAsBytes)
        copyPermissions(template.source.toPath(), template.destination.toPath())
        info(
            "Copied '$component' template ../${template.source.parentFile.name}/${template.source.name} -> ${template.fileName}"
        )
    }

    private fun writeConfig(file: File, component: String, properties: Collection<Property>) {
        if (properties.isEmpty()) return
        configIo.writeTo(file).write(properties)
        info("Generated $component/${file.name}")
    }

    private fun deleteObsoleteConfigs(result: File, empty: Boolean, protected: Set<Path>) {
        if (empty && !isProtected(result, protected)) deleteConfig(result)
        val obsolete = oppositeConfig(result)
        if (isProtected(obsolete, protected)) return
        deleteConfig(obsolete)
        deleteConfig(File(result.parentFile, "diff-${obsolete.name}"))
    }

    private fun deleteConfig(file: File) {
        try {
            Files.deleteIfExists(file.toPath())
        } catch (error: IOException) {
            throw IllegalStateException("Can't remove obsolete config '$file'", error)
        }
    }
}
