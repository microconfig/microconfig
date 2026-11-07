package io.microconfig.core.environments.repository

import io.microconfig.core.environments.Component
import io.microconfig.core.environments.ComponentFactory
import io.microconfig.core.environments.repository.impl.ComponentDefinition
import io.microconfig.core.environments.repository.impl.ComponentDefinition.Companion.withAlias
import io.microconfig.core.environments.repository.impl.ComponentDefinition.Companion.withName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ComponentDefinitionTest {
    val alias: String = "alias"

    val name: String = "name"

    @Test
    fun factoryMethods() {
        assertEquals(ComponentDefinition(alias, name), withAlias(alias, name))
        assertEquals(ComponentDefinition(name, name), withName(name))
    }

    @Test
    fun toComponent() {
        val env = "dev"
        val factory: ComponentFactory = mock(ComponentFactory::class.java)
        val expected: Component = mock(Component::class.java)
        `when`(factory.createComponent(alias, name, env)).thenReturn(expected)
        assertSame(expected, withAlias(alias, name).toComponent(factory, env))
    }
}
