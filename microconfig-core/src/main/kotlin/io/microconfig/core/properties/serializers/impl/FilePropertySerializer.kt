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
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

internal class FilePropertySerializer(
    private val directory: File,
    private val listener: BiConsumer<File, Collection<Property>>,
    private val configIo: ConfigIo,
) : PropertySerializer<File> {
    override fun prepare(properties: List<TypedProperties>): PropertySerializer<File> {
        // No writes occur here; later cleanup still resolves the current filesystem paths.
        val prefixes = ConcurrentHashMap<Path, Path>()
        val outputs = properties.parallelStream().map { unit -> runCatching { output(unit, prefixes) } }
            .toList().map { it.getOrThrow() }
        return prepareOutputs(outputs)
    }

    override fun serialize(
        properties: Collection<Property>,
        templates: List<Template>,
        configType: ConfigType,
        componentName: String,
        environment: String,
    ): File {
        val target = output(properties, templates, configType, componentName, environment)
        val prepared = prepareOutputs(listOf(target))
        return prepared.serialize(properties, templates, configType, componentName, environment)
    }

    private fun output(unit: TypedProperties, prefixes: MutableMap<Path, Path>): Output =
        output(unit.properties, unit.templates, unit.configType,
            unit.declaringComponent.component, unit.declaringComponent.environment, prefixes)

    private fun output(
        properties: Collection<Property>,
        templates: List<Template>,
        type: ConfigType,
        component: String,
        environment: String,
        prefixes: MutableMap<Path, Path>? = null,
    ): Output {
        val values = properties.toList()
        val file = resultFile(component, type, values, values.none { !it.isVar })
        return Output(file, outputPath(file, prefixes), values, templates.toList(), component, environment, type)
    }

    private fun resultFile(
        component: String,
        type: ConfigType,
        properties: Collection<Property>,
        empty: Boolean,
    ): File {
        val format = resultFormat(properties)
        val preferred = File(directory, "$component/${type.resultFileName}${format.extension()}")
        if (!empty || preferred.isFile) return preferred
        val previous = oppositeConfig(preferred)
        return if (previous.isFile) previous else preferred
    }

    private fun resultFormat(properties: Collection<Property>) =
        if (properties.isEmpty() || properties.any { it.configFormat == YAML }) YAML else PROPERTIES

    private fun oppositeConfig(file: File): File =
        File(
            file.parentFile,
            "${file.nameWithoutExtension}.${if (file.extension == "yaml") "properties" else "yaml"}",
        )

    private fun outputPath(file: File, prefixes: MutableMap<Path, Path>? = null): Path =
        try {
            val path = file.toPath().toAbsolutePath()
            if (path.any { it.toString() == "." || it.toString() == ".." })
                resolveLinks(path, mutableSetOf())
            else resolveExistingPrefix(path, prefixes)
        } catch (error: IOException) {
            throw UncheckedIOException("Can't resolve output '$file'", error)
        }

    private fun resolveExistingPrefix(path: Path, prefixes: MutableMap<Path, Path>?): Path {
        var existing = path
        val suffix = ArrayDeque<Path>()
        while (existing.parent != null && !Files.exists(existing, NOFOLLOW_LINKS)) {
            suffix.addFirst(existing.fileName)
            existing = existing.parent
        }
        // A dangling or cyclic final symlink still needs the segment-wise checks.
        if (Files.isSymbolicLink(existing)) return resolveLinks(path, mutableSetOf())
        var resolved = prefixes?.computeIfAbsent(existing) { it.toRealPath() } ?: existing.toRealPath()
        for (part in suffix) resolved = resolved.resolve(part)
        return resolved
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

    private fun prepareOutputs(outputs: List<Output>): PropertySerializer<File> {
        val protected = validateOutputs(outputs)
        val notifications = prepareNotifications(outputs, protected)
        return preparedSerializer(outputs, protected, notifications)
    }

    private fun validateOutputs(outputs: List<Output>): Set<Path> {
        val destinations = OutputDestinations()
        val protected = mutableSetOf<Path>()
        outputs.forEach { output ->
            destinations.checkName(output.file, output.path)
            output.templates.forEach { template ->
                reserveOutput(template.destination, destinations)
                protected.addAll(protectedPaths(template.destination))
            }
            if (output.visible.isNotEmpty()) {
                reserveOutput(output.file, destinations, output.path)
                protected.add(output.file.toPath().toAbsolutePath().normalize())
                protected.add(output.path)
            }
        }
        return protected
    }

    private fun reserveOutput(
        file: File,
        destinations: OutputDestinations,
        path: Path = outputPath(file),
    ) = destinations.reserve(file, path)

    private fun protectedPaths(file: File): Set<Path> =
        setOf(file.toPath().toAbsolutePath().normalize(), outputPath(file))

    private fun prepareNotifications(outputs: List<Output>, protected: Set<Path>): Map<Path, () -> Unit> {
        if (listener !is ConfigChangeListener) return emptyMap()
        val selected = outputs.filter { it.visible.isNotEmpty() || !isProtected(it.file, protected, it.path) }
            .distinctBy { it.path }
        val notifications = listener.prepareAll(selected.associate { it.file to it.properties })
        return selected.associate { it.path to notifications.getValue(it.file) }
    }

    private fun isProtected(file: File, protected: Set<Path>, resolved: Path? = null): Boolean =
        if (Files.isSymbolicLink(file.toPath()))
            file.toPath().toAbsolutePath().normalize() in protected
        else (resolved ?: outputPath(file)) in protected

    private fun preparedSerializer(
        outputs: List<Output>,
        protected: Set<Path>,
        notifications: Map<Path, () -> Unit>,
    ): PropertySerializer<File> {
        val byScope = outputs.groupBy { Triple(it.component, it.environment, it.type.typeName) }
        val generated = outputs.filter { it.visible.isNotEmpty() }.map { it.path }.toSet()
        val templateConfigs =
            if (outputs.none { it.templates.isNotEmpty() }) emptyMap()
            else outputs.flatMap { listOf(it.file, oppositeConfig(it.file)) }
                .groupBy(::outputPath).filterKeys { it !in generated }
        val completed = ConcurrentHashMap.newKeySet<Path>()
        return object : PropertySerializer<File> {
            override fun prepare(properties: List<TypedProperties>): PropertySerializer<File> =
                this@FilePropertySerializer.prepare(properties)

            override fun serialize(
                properties: Collection<Property>,
                templates: List<Template>,
                configType: ConfigType,
                componentName: String,
                environment: String,
            ): File {
                val output =
                    byScope[Triple(componentName, environment, configType.typeName)]?.firstOrNull {
                        it.type.resultFileName == configType.resultFileName &&
                            sameProperties(it, properties) && sameTemplates(it.templates, templates)
                    }
                requireNotNull(output) { "Output was not prepared for $componentName[$environment]" }
                val notify = notificationFor(output, protected, notifications, completed)
                writeOutput(output, protected)
                deleteStaleTemplateDiffs(output.templates, templateConfigs)
                notify()
                return output.file
            }
        }
    }

    private fun sameProperties(output: Output, properties: Collection<Property>): Boolean =
        output.properties == properties.toList() ||
            resultFormat(output.properties) == resultFormat(properties) &&
                configContent(output.file, output.properties).contentEquals(configContent(output.file, properties))

    private fun configContent(file: File, properties: Collection<Property>): ByteArray =
        configIo
            .writeTo(file)
            .serialize(properties.filterNot { it.isVar })
            .toByteArray(Charsets.UTF_8)

    private fun sameTemplates(first: List<Template>, second: List<Template>): Boolean =
        first.size == second.size && first.zip(second).all { (left, right) ->
            left === right || left.source == right.source && left.destination == right.destination &&
                left.contentAsBytes.contentEquals(right.contentAsBytes)
        }

    private fun notificationFor(
        output: Output,
        protected: Set<Path>,
        notifications: Map<Path, () -> Unit>,
        completed: MutableSet<Path>,
    ): () -> Unit {
        if (output.visible.isEmpty() && isProtected(output.file, protected) ||
            !completed.add(output.path)) return {}
        notifications[output.path]?.let { return it }
        listener.accept(output.file, output.properties)
        return {}
    }

    private fun writeOutput(output: Output, protected: Set<Path>) {
        output.templates.forEach { copyTemplate(it, output.component) }
        writeConfig(output.file, output.component, output.visible)
        deleteObsoleteConfigs(output.file, output.visible.isEmpty(), protected)
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
        configIo.writeTo(file).write(properties)
        info("Generated $component/${file.name}")
    }

    private fun deleteObsoleteConfigs(result: File, empty: Boolean, protected: Set<Path>) {
        if (empty && !Files.notExists(result.toPath(), NOFOLLOW_LINKS) &&
            !isProtected(result, protected)) deleteConfig(result)
        val obsolete = oppositeConfig(result)
        val diff = File(result.parentFile, "diff-${obsolete.name}")
        if (Files.notExists(obsolete.toPath(), NOFOLLOW_LINKS) &&
            Files.notExists(diff.toPath(), NOFOLLOW_LINKS)) return
        if (isProtected(obsolete, protected)) return
        deleteConfig(obsolete)
        deleteConfig(diff)
    }

    private fun deleteStaleTemplateDiffs(templates: List<Template>, configs: Map<Path, List<File>>) {
        templates.forEach { template ->
            configs[outputPath(template.destination)].orEmpty().forEach { file ->
                deleteConfig(File(file.parentFile, "diff-${file.name}"))
            }
        }
    }

    private fun deleteConfig(file: File) {
        try {
            Files.deleteIfExists(file.toPath())
        } catch (error: IOException) {
            throw IllegalStateException("Can't remove obsolete config '$file'", error)
        }
    }

    private data class Output(
        val file: File,
        val path: Path,
        val properties: List<Property>,
        val templates: List<Template>,
        val component: String,
        val environment: String,
        val type: ConfigType,
    ) {
        val visible: List<Property> = properties.filterNot { it.isVar }
    }
}
