package io.microconfig.core.properties.repository.impl

import io.microconfig.core.configtypes.ConfigType
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.yaml.YamlScalars

class CompositePropertiesRepository(private val repositories: List<PropertiesRepository>) :
    PropertiesRepository {
    override fun getPropertiesOf(
        originalComponentName: String,
        environment: String,
        configType: ConfigType,
    ): Map<String, Property> {
        val properties = repositories.flatMap { repository ->
            YamlScalars.indexProperties(
                repository.getPropertiesOf(originalComponentName, environment, configType).values.toList()
            ).values
        }
        return YamlScalars.indexProperties(properties, overwrite = false)
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
