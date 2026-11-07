package io.microconfig.core.environments

import io.microconfig.core.environments.impl.ComponentGroupImpl
import java.util.Optional.empty
import java.util.Optional.of
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ComponentGroupImplTest {
    val one: Component = mock(Component::class.java)

    val two: Component = mock(Component::class.java)

    val components: Components = mock(Components::class.java)

    @Test
    fun findComponentByName() {
        `when`(one.name).thenReturn("one")
        `when`(two.name).thenReturn("two")
        `when`(components.asList()).thenReturn(listOf(one, two))
        val group: ComponentGroup = ComponentGroupImpl("group", components)
        assertEquals(of(two), group.findComponentWithName("two"))
        assertEquals(empty<Any>(), group.findComponentWithName("missing"))
    }

    @Test
    fun string() {
        `when`(components.toString()).thenReturn("[]")
        val group: ComponentGroup = ComponentGroupImpl("groupName", components)
        assertEquals("groupName: []", group.toString())
    }
}
