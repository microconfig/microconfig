package io.microconfig.core.configtypes

import java.io.File

object ConfigTypeFilters {
    fun eachConfigType(): ConfigTypeFilter = ConfigTypeFilter { it }

    fun configType(vararg types: ConfigType): ConfigTypeFilter = ConfigTypeFilter {
        require(types.isNotEmpty()) { "No config types provided" }
        types.toList()
    }

    fun configTypeWithName(vararg name: String): ConfigTypeFilter {
        val names = name.toSet()
        require(names.isNotEmpty()) { "No config types provided" }
        return ConfigTypeFilter { types -> selectNamedTypes(names, types) }
    }

    private fun selectNamedTypes(names: Set<String>, types: List<ConfigType>): List<ConfigType> {
        val unsupported = names - types.map { it.name }.toSet()
        require(unsupported.isEmpty()) {
            "Unsupported config types: $unsupported Configured types: $types"
        }
        return types.filter { it.name in names }
    }

    fun configTypeWithExtensionOf(file: File): ConfigTypeFilter {
        val extension = file.extension.takeIf { it.isNotEmpty() }?.let { ".$it" }
        require(extension != null) {
            "File $file doesn't have an extension. Unable to resolve component type."
        }
        return ConfigTypeFilter { types -> selectByExtension(extension, types) }
    }

    private fun selectByExtension(extension: String, types: List<ConfigType>): List<ConfigType> =
        listOf(
            types.firstOrNull { extension in it.sourceExtensions }
                ?: throw IllegalArgumentException("Unsupported config extension '$extension'")
        )
}
