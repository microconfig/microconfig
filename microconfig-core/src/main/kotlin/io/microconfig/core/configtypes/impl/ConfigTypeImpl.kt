package io.microconfig.core.configtypes.impl

import io.microconfig.core.configtypes.ConfigType

data class ConfigTypeImpl(
    private val typeName: String,
    override val sourceExtensions: Set<String>,
    override val resultFileName: String,
) : ConfigType {
    override fun getName(): String = typeName

    companion object {
        fun byName(name: String): ConfigType = byNameAndExtensions(name, setOf(".$name"), name)

        fun byNameAndExtensions(
            name: String,
            extensions: Set<String>,
            resultFileName: String,
        ): ConfigType {
            val invalid = extensions.filterNot { it.startsWith('.') }
            require(invalid.isEmpty()) { "File extension must start with '.'. Current: $invalid" }
            return ConfigTypeImpl(name, extensions, resultFileName)
        }
    }
}
