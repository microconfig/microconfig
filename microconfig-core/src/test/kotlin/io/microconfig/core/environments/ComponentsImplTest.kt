package io.microconfig.core.environments

import io.microconfig.core.configtypes.ConfigTypeFilter
import io.microconfig.core.environments.impl.ComponentsImpl
import io.microconfig.core.properties.Properties
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.impl.PropertiesImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ComponentsImplTest {
    val one: Component = mock(Component::class.java)

    val oneProps: Properties = mock(Properties::class.java)

    val oneTProps: TypedProperties = mock(TypedProperties::class.java)

    val two: Component = mock(Component::class.java)

    val twoProps: Properties = mock(Properties::class.java)

    val twoTProps: TypedProperties = mock(TypedProperties::class.java)

    val factory: PropertiesFactory = mock(PropertiesFactory::class.java)

    val subj: Components = ComponentsImpl(listOf(one, two), factory)

    @Test
    fun filterPropertiesForContainedComponents() {
        val filter: ConfigTypeFilter = mock(ConfigTypeFilter::class.java)
        `when`(one.getPropertiesFor(filter)).thenReturn(oneProps)
        `when`(two.getPropertiesFor(filter)).thenReturn(twoProps)
        `when`(oneProps.asTypedProperties()).thenReturn(listOf(oneTProps))
        `when`(twoProps.asTypedProperties()).thenReturn(listOf(twoTProps))
        val expected: Properties = PropertiesImpl(listOf(oneTProps, twoTProps))
        `when`(factory.flat(listOf(oneProps, twoProps))).thenReturn(expected)
        assertEquals(expected, subj.getPropertiesFor(filter))
    }

    @Test
    fun string() {
        `when`(one.toString()).thenReturn("one")
        `when`(two.toString()).thenReturn("two")
        assertEquals("[one, two]", subj.toString())
    }
}
