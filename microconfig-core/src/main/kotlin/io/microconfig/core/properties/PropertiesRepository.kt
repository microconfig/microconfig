package io.microconfig.core.properties

import io.microconfig.core.configtypes.ConfigType

fun interface PropertiesRepository {
    fun getPropertiesOf(
        originalComponentName: String,
        environment: String,
        configType: ConfigType,
    ): Map<String, Property>
}
