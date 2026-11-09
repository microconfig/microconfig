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
import io.microconfig.utils.FileUtils.writeWithPermissions
import io.microconfig.utils.Logger.info
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
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
        output(
            unit.properties, unit.templates, unit.configType,
            unit.declaringComponent.component, unit.declaringComponent.environment, prefixes
        )

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
            resolveExistingPrefix(file.toPath().toAbsolutePath().normalize(), prefixes)
        } catch (error: IOException) {
            throw UncheckedIOException("Can't resolve output '$file'", error)
        }

    private fun resolveExistingPrefix(path: Path, prefixes: MutableMap<Path, Path>?): Path {
        var existing = path
        val suffix = ArrayDeque<Path>()
        while (existing.parent != null && !Files.exists(existing)) {
            suffix.addFirst(existing.fileName)
            existing = existing.parent
        }
        var resolved = prefixes?.computeIfAbsent(existing) { it.toRealPath() } ?: existing.toRealPath()
        for (part in suffix) resolved = resolved.resolve(part)
        return resolved
    }

    private fun prepareOutputs(outputs: List<Output>): PropertySerializer<File> {
        val componentDirectories = outputs.map { it.component }.distinct().map(::componentDirectory)
            .associateWith { outputPath(it) }
        val protected = validateOutputs(outputs, componentDirectories)
        val notifications = prepareNotifications(outputs, protected, componentDirectories)
        return preparedSerializer(outputs, protected, notifications, componentDirectories)
    }

    private fun componentDirectory(component: String): File = File(directory, component)

    private fun validateOutputs(outputs: List<Output>, componentDirectories: Map<File, Path>): Set<Path> {
        val destinations = OutputDestinations()
        val protected = mutableSetOf<Path>()
        componentDirectories.values.forEach { path ->
            destinations.reserveDirectory(path)
            protected.addAll(generateSequence(path) { it.parent })
        }
        if (protected.any { it.fileName?.toString()?.startsWith("diff-", ignoreCase = true) == true }) {
            checkDiffDirectories(outputs, componentDirectories, destinations)
        }
        outputs.forEach { output ->
            destinations.checkName(output.file, output.path)
            output.templates.forEach { template ->
                val path = outputPath(template.destination)
                destinations.reserve(template.destination, path)
                protected.add(path)
            }
            if (output.visible.isNotEmpty()) {
                destinations.reserve(output.file, output.path)
                protected.add(output.path)
            }
        }
        return protected
    }

    private fun checkDiffDirectories(
        outputs: List<Output>,
        componentDirectories: Map<File, Path>,
        destinations: OutputDestinations,
    ) {
        outputs.forEach { output ->
            val parent = output.file.parentFile
            val resolved = componentDirectories[parent] ?: outputPath(parent)
            destinations.checkDiff(output.file, resolved)
            destinations.checkDiff(oppositeConfig(output.file), resolved)
        }
    }

    private fun prepareNotifications(
        outputs: List<Output>,
        protected: Set<Path>,
        componentDirectories: Map<File, Path>,
    ): Map<Path, () -> Unit> {
        if (listener !is ConfigChangeListener) return emptyMap()
        val selected = outputs.filter { it.visible.isNotEmpty() || it.path !in protected }
            .distinctBy { it.path }
        val notifications = listener.prepareAll(
            selected.associate { it.file to it.properties },
            selected.associate { it.file to componentDirectories.getValue(componentDirectory(it.component)).toFile() },
        )
        return selected.associate { it.path to notifications.getValue(it.file) }
    }

    private fun preparedSerializer(
        outputs: List<Output>,
        protected: Set<Path>,
        notifications: Map<Path, () -> Unit>,
        componentDirectories: Map<File, Path>,
    ): PropertySerializer<File> {
        val byScope = outputs.groupBy { Triple(it.component, it.environment, it.type.typeName) }
        val generated = outputs.filter { it.visible.isNotEmpty() }.map { it.path }.toSet()
        val templateConfigs =
            if (outputs.none { it.templates.isNotEmpty() }) emptyMap()
            else outputs.flatMap { listOf(it.file, oppositeConfig(it.file)) }
                .groupBy(::outputPath).filterKeys { it !in generated }
        val completed = ConcurrentHashMap.newKeySet<Path>()
        val createdDirectories = ConcurrentHashMap<Path, Path>()
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
                val path = componentDirectories.getValue(componentDirectory(componentName))
                createdDirectories.computeIfAbsent(path, ::createComponentDirectory)
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
        if (output.visible.isEmpty() && output.path in protected ||
            !completed.add(output.path)
        ) return {}
        notifications[output.path]?.let { return it }
        listener.accept(output.file, output.properties)
        return {}
    }

    private fun createComponentDirectory(path: Path): Path =
        try {
            Files.createDirectories(path)
        } catch (error: IOException) {
            throw UncheckedIOException("Can't create component output directory '$path'", error)
        }

    private fun writeOutput(output: Output, protected: Set<Path>) {
        output.templates.forEach { copyTemplate(it, output.component) }
        writeConfig(output.file, output.component, output.visible)
        deleteObsoleteConfigs(output.file, output.visible.isEmpty(), protected)
    }

    private fun copyTemplate(template: Template, component: String) {
        writeWithPermissions(template.destination.toPath(), template.contentAsBytes, template.source.toPath())
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
        if (empty && !Files.notExists(result.toPath()) &&
            outputPath(result) !in protected
        ) deleteConfig(result)
        val obsolete = oppositeConfig(result)
        val diff = File(result.parentFile, "diff-${obsolete.name}")
        if (Files.notExists(obsolete.toPath()) &&
            Files.notExists(diff.toPath())
        ) return
        if (outputPath(obsolete) in protected) return
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
