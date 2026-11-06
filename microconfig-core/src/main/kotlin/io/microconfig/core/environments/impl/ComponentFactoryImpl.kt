package io.microconfig.core.environments.impl

import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.properties.PropertiesFactory

class ComponentFactoryImpl(
    private val configTypes: ConfigTypeRepository,
    private val properties: PropertiesFactory,
) : ComponentFactory {
    override fun createComponent(name: String, type: String, environment: String): Component =
        ComponentImpl(configTypes, properties, name, type, environment)
}
