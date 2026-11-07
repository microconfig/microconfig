package io.microconfig.core.environments

import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.environments.impl.ComponentFactoryImpl
import io.microconfig.core.environments.impl.ComponentImpl
import io.microconfig.core.properties.PropertiesFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ComponentFactoryImplTest {
    val configTypeRepository: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    val propertiesFactory: PropertiesFactory = mock(PropertiesFactory::class.java)

    val subj: ComponentFactory = ComponentFactoryImpl(configTypeRepository, propertiesFactory)

    @Test
    fun createComponent() {
        val name = "name"
        val originalName = "originalName"
        val env = "env"
        assertEquals(
            ComponentImpl(configTypeRepository, propertiesFactory, name, originalName, env),
            subj.createComponent(name, originalName, env),
        )
    }
}
