package io.microconfig.core.configtypes.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.configtypes.ConfigTypeRepository

class FirstNonEmptyConfigTypeRepository(private val repositories: List<ConfigTypeRepository>) :
    ConfigTypeRepository {
    override val configTypes: List<ConfigType>
        get() =
            repositories.asSequence().map { it.configTypes }.firstOrNull { it.isNotEmpty() }
                ?: error("Config types are not configured")

    companion object {
        fun composite(vararg repositories: ConfigTypeRepository): ConfigTypeRepository =
            FirstNonEmptyConfigTypeRepository(repositories.toList())
    }
}
