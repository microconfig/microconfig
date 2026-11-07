package io.microconfig.core.environments.repository.impl

import io.microconfig.core.environments.repository.EnvironmentException
import io.microconfig.io.FsReader
import org.yaml.snakeyaml.Yaml
import java.io.File

internal class EnvironmentFile(private val file: File) {
    fun parseUsing(reader: FsReader): EnvironmentDefinition =
        try {
            parseDescriptor(readDescriptor(reader))
        } catch (error: RuntimeException) {
            throw EnvironmentException(
                "Can't parse environment '${file.nameWithoutExtension}'",
                error,
            )
        }

    private fun readDescriptor(reader: FsReader): Map<String, Any?> =
        requireNotNull(Yaml().load<Map<String, Any?>>(reader.readFully(file))) {
            "Environment descriptor must contain a mapping"
        }

    private fun parseDescriptor(descriptor: Map<String, Any?>): EnvironmentDefinition {
        val include = parseInclude(descriptor["include"])
        val profiles = parseProfiles(descriptor["profiles"])
        val abstract = descriptor["abstract"] as Boolean? ?: false
        val groups =
            descriptor
                .filterKeys { it !in descriptorKeys }
                .map { (name, value) -> parseGroup(name, value) }
        return EnvironmentDefinition(
            file,
            file.nameWithoutExtension,
            abstract,
            profiles,
            include,
            groups,
        )
    }

    private fun parseInclude(value: Any?): EnvInclude {
        if (value == null) return EnvInclude.empty()
        require(value is Map<*, *>) { "Environment include must be a mapping" }
        val name = value["env"] as String
        val excludes =
            (value["exclude"] as Collection<*>?)?.map { it as String }?.toSet() ?: emptySet()
        return EnvInclude(name, excludes)
    }

    private fun parseProfiles(value: Any?): List<String> =
        when (value) {
            null -> emptyList()
            is Collection<*> -> value.map { it.toString() }
            else -> listOf(value.toString())
        }

    private fun parseGroup(
        name: String,
        value: Any?,
    ): ComponentGroupDefinition =
        try {
            val properties = value as Map<*, *>
            ComponentGroupDefinition(
                name,
                parseComponents(properties["components"]),
                parseComponents(properties["exclude"]),
                parseComponents(properties["append"]),
            )
        } catch (error: RuntimeException) {
            throw EnvironmentException("Can't parse group declaration: '$name=$value'", error)
        }

    private fun parseComponents(value: Any?): List<ComponentDefinition> =
        if (value == null) emptyList()
        else (value as List<*>).filterNotNull().map { parseComponent(it as String) }

    private fun parseComponent(value: String): ComponentDefinition {
        val parts = value.split(':').dropLastWhile { it.isEmpty() }
        require(parts.size in 1..2) { "Incorrect component declaration: $value" }
        return if (parts.size == 1) ComponentDefinition.withName(parts.single())
        else ComponentDefinition.withAlias(parts[0], parts[1])
    }

    companion object {
        private val descriptorKeys = setOf("include", "profiles", "abstract")
    }
}
