package io.microconfig.core.environments

import io.microconfig.core.environments.impl.ComponentsImpl
import io.microconfig.core.environments.impl.EnvironmentImpl
import io.microconfig.core.properties.PropertiesFactory
import io.microconfig.core.properties.repository.ComponentNotFoundException
import java.util.Optional.empty
import java.util.Optional.of
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class EnvironmentImplTest {
    val one: Component = mock(Component::class.java)

    val two: Component = mock(Component::class.java)

    val three: Component = mock(Component::class.java)

    val components1: Components = mock(Components::class.java)

    val components2: Components = mock(Components::class.java)

    val group1: ComponentGroup = mock(ComponentGroup::class.java)

    val group2: ComponentGroup = mock(ComponentGroup::class.java)

    val groups: List<ComponentGroup> = listOf(group1, group2)

    val factory: ComponentFactory = mock(ComponentFactory::class.java)

    val factory2: PropertiesFactory = mock(PropertiesFactory::class.java)

    val env: Environment =
        EnvironmentImpl(null, "dev", false, emptyList(), groups, factory, factory2)

    @BeforeEach
    fun setUp() {
        `when`(one.name).thenReturn("one")
        `when`(two.name).thenReturn("two")
        `when`(three.name).thenReturn("three")
        `when`(group1.name).thenReturn("group1")
        `when`(group2.name).thenReturn("group2")
        `when`(group1.components).thenReturn(components1)
        `when`(group2.components).thenReturn(components2)
        `when`(components1.asList()).thenReturn(listOf(one, two))
        `when`(components2.asList()).thenReturn(listOf(three))
    }

    @Test
    fun findGroupByName() {
        assertEquals(group2, env.getGroupWithName("group2"))
        assertThrows(IllegalArgumentException::class.java, { env.getGroupWithName("group3") })
    }

    @Test
    fun findGroupWithComponentName() {
        `when`(group2.findComponentWithName("three")).thenReturn(of(three))
        assertEquals(of(group2), env.findGroupWithComponent("three"))
        assertEquals(empty<Any>(), env.findGroupWithComponent("not found"))
    }

    @Test
    fun getAllComponents() {
        assertEquals(ComponentsImpl(listOf(one, two, three), factory2), env.allComponents)
    }

    @Test
    fun getComponentWithName() {
        `when`(group1.findComponentWithName("two")).thenReturn(of(two))
        assertEquals(two, env.getComponentWithName("two"))
        assertThrows(ComponentNotFoundException::class.java, { env.getComponentWithName("four") })
    }

    @Test
    fun getOrCreateComponentWithName() {
        `when`(group1.findComponentWithName("two")).thenReturn(of(two))
        assertEquals(two, env.findComponentWithName("two"))
        val four: Component = mock(Component::class.java)
        `when`(factory.createComponent("four", "four", env.name)).thenReturn(four)
        assertEquals(four, env.findComponentWithName("four"))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun useTheFirstSuccessfulComponentLookup(createMissing: Boolean) {
        `when`(group2.findComponentWithName("three")).thenReturn(of(three), empty())

        val component = if (createMissing) env.findComponentWithName("three")
            else env.getComponentWithName("three")

        assertSame(three, component)
    }

    @Test
    fun findComponentsFrom() {
        assertEquals(components(one, two, three), env.findComponentsFrom(emptyList(), emptyList()))
        assertEquals(
            components(one, two, three),
            env.findComponentsFrom(listOf("group1", "group2"), emptyList()),
        )
        assertEquals(
            components(one, two),
            env.findComponentsFrom(listOf("group1", "group2"), listOf("one", "two")),
        )
        assertEquals(
            components(one, two),
            env.findComponentsFrom(listOf("group1"), listOf("one", "two")),
        )
        assertEquals(
            components(one, three),
            env.findComponentsFrom(emptyList(), listOf("one", "three")),
        )
        assertThrows(
            IllegalArgumentException::class.java,
            { env.findComponentsFrom(listOf("bad"), listOf("bad")) },
        )
        assertThrows(
            IllegalArgumentException::class.java,
            { env.findComponentsFrom(listOf("bad"), emptyList()) },
        )
        assertThrows(
            ComponentNotFoundException::class.java,
            { env.findComponentsFrom(emptyList(), listOf("bad")) },
        )
    }

    @ParameterizedTest
    @CsvSource("false, false", "true, false", "false, true", "true, true")
    fun rejectDuplicateComponentNames(filterGroups: Boolean, filterComponents: Boolean) {
        `when`(one.name).thenReturn("orders")
        `when`(three.name).thenReturn("orders")
        val selectedGroups = if (filterGroups) listOf("group1", "group2") else emptyList()
        val selectedComponents = if (filterComponents) listOf("orders") else emptyList()

        val error = assertThrows(IllegalStateException::class.java) {
            env.findComponentsFrom(selectedGroups, selectedComponents)
        }

        assertEquals("Duplicate component declarations in 'dev' env: [orders]", error.message)
    }

    @Test
    fun reportAllDuplicateComponentNames() {
        `when`(components2.asList()).thenReturn(listOf(three, one, two))

        val error = assertThrows(IllegalStateException::class.java) {
            env.findComponentsFrom(emptyList(), emptyList())
        }

        assertEquals("Duplicate component declarations in 'dev' env: [one, two]", error.message)
    }

    @Test
    fun ignoreDuplicateComponentNamesOutsideSelectedGroups() {
        `when`(three.name).thenReturn("one")

        assertEquals(components(one, two), env.findComponentsFrom(listOf("group1"), emptyList()))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun ignoreRepeatedGroupFiltersAndPreserveSelectionOrder(filterComponents: Boolean) {
        val requested = if (filterComponents) listOf("two", "three") else emptyList()
        val expected = if (filterComponents) components(two, three) else components(three, one, two)

        assertEquals(
            expected,
            env.findComponentsFrom(listOf("group2", "group1", "group2"), requested),
        )
    }

    @Test
    fun testToString() {
        `when`(group1.toString()).thenReturn("g1")
        `when`(group2.toString()).thenReturn("g2")
        assertEquals("dev: [g1, g2]", env.toString())
    }

    private fun components(vararg components: Component): Components {
        return ComponentsImpl(components.toList(), factory2)
    }
}
