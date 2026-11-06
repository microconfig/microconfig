package io.microconfig.core.environments

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.properties.Properties

interface Component {
    val name: String
    val originalName: String
    val environment: String

    fun getPropertiesFor(filter: ConfigTypeFilter): Properties
}
