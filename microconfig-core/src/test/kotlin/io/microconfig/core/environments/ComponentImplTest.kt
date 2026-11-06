package io.microconfig.core.environments

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.configtypes.ConfigTypeRepository
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.StandardConfigType.DEPLOY
import io.microconfig.core.environments.impl.ComponentImpl
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ComponentImplTest {
    val configTypeRepository: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    val propertiesFactory: PropertiesFactory = mock(PropertiesFactory::class.java)

    val filter: ConfigTypeFilter = mock(ConfigTypeFilter::class.java)

    val subj: Component =
        ComponentImpl(configTypeRepository, propertiesFactory, "name", "original", "env")

    @Test
    fun filterProperties() {
        val properties: Properties = mock(Properties::class.java)
        `when`(configTypeRepository.configTypes).thenReturn(listOf(APPLICATION, DEPLOY))
        `when`(filter.selectTypes(listOf(APPLICATION, DEPLOY))).thenReturn(listOf(DEPLOY))
        `when`(propertiesFactory.getPropertiesOf("name", "original", "env", listOf(DEPLOY)))
            .thenReturn(properties)
        assertEquals(properties, subj.getPropertiesFor(filter))
    }

    @Test
    fun string() {
        assertEquals("name", subj.toString())
    }
}
