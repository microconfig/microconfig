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
    fun ipOptionalWrapping() {
        val nullableIp: ComponentGroup = ComponentGroupImpl("null", null, components)
        assertEquals(empty<Any>(), nullableIp.ip)
        val ip: String = "127.0.0.1"
        val ipGroup: ComponentGroup = ComponentGroupImpl("group", ip, components)
        assertEquals(of(ip), ipGroup.ip)
    }

    @Test
    fun findComponentByName() {
        `when`(one.name).thenReturn("one")
        `when`(two.name).thenReturn("two")
        `when`(components.asList()).thenReturn(listOf(one, two))
        val group: ComponentGroup = ComponentGroupImpl("group", null, components)
        assertEquals(of(two), group.findComponentWithName("two"))
        assertEquals(empty<Any>(), group.findComponentWithName("missing"))
    }

    @Test
    fun string() {
        `when`(components.toString()).thenReturn("[]")
        val group: ComponentGroup = ComponentGroupImpl("groupName", null, components)
        assertEquals("groupName: []", group.toString())
    }
}
