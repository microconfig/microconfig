package io.microconfig.core.environments

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.properties.Properties

interface Components {
    fun asList(): List<Component>

    fun getPropertiesFor(filter: ConfigTypeFilter): Properties
}
