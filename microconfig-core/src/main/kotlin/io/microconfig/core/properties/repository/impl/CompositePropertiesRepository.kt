package io.microconfig.core.properties.repository.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.Property

class CompositePropertiesRepository(private val repositories: List<PropertiesRepository>) :
    PropertiesRepository {
    override fun getPropertiesOf(
        originalComponentName: String,
        environment: String,
        configType: ConfigType,
    ): Map<String, Property> = buildMap {
        repositories.forEach { repository ->
            repository.getPropertiesOf(originalComponentName, environment, configType).forEach {
                (key, value) ->
                putIfAbsent(key, value)
            }
        }
    }

    companion object {
        fun compositeOf(
            repositories: List<PropertiesRepository>,
            vararg additional: PropertiesRepository,
        ): PropertiesRepository {
            val combined = repositories + additional
            return combined.singleOrNull() ?: CompositePropertiesRepository(combined)
        }
    }
}
