package io.microconfig.core.properties

import io.microconfig.core.configtypes.ConfigType

interface PropertiesFactory {
    fun getPropertiesOf(
        componentName: String,
        componentOriginalName: String,
        environment: String,
        configTypes: List<ConfigType>,
    ): Properties

    fun flat(properties: List<Properties>): Properties
}
