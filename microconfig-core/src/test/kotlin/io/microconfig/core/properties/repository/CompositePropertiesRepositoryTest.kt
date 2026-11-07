package io.microconfig.core.properties.repository

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.repository.impl.CompositePropertiesRepository.Companion.compositeOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Arrays.stream
import java.util.function.Function.identity
import java.util.stream.Collectors.toMap

class CompositePropertiesRepositoryTest {
    val first: PropertiesRepository = mock(PropertiesRepository::class.java)

    val second: PropertiesRepository = mock(PropertiesRepository::class.java)

    val subj: PropertiesRepository = compositeOf(listOf(first), second)

    val one: Property = property("foo.bar", "value", PROPERTIES, null)

    val two: Property = property("foo.bar", "default-value", PROPERTIES, null)

    val three: Property = property("foo.baz", "value", PROPERTIES, null)

    @Test
    fun should_merge_in_order() {
        `when`(first.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(propsMap(one))
        `when`(second.getPropertiesOf("component", "dev", APPLICATION))
            .thenReturn(propsMap(two, three))
        val expected: Map<String, Property> = propsMap(one, three)
        val result: Map<String, Property> = subj.getPropertiesOf("component", "dev", APPLICATION)
        assertEquals(expected, result)
    }

    @Test
    fun should_return_empty_map_if_unresolved() {
        `when`(first.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(emptyMap())
        `when`(second.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(emptyMap())
        val result: Map<String, Property> = subj.getPropertiesOf("component", "dev", APPLICATION)
        assertEquals(emptyMap<Any, Any>(), result)
    }

    @Test
    fun captureFirstRepositoryValuesBeforeReadingTheNextProvider() {
        val properties = linkedMapOf(one.key to one)
        `when`(first.getPropertiesOf("component", "dev", APPLICATION)).thenReturn(properties)
        `when`(second.getPropertiesOf("component", "dev", APPLICATION)).thenAnswer {
            properties[one.key] = two
            emptyMap<String, Property>()
        }

        val result = subj.getPropertiesOf("component", "dev", APPLICATION)

        assertSame(one, result.getValue(one.key))
    }

    @Test
    fun should_return_same_repository_if_only_one_supplied() {
        val repo: PropertiesRepository = mock(PropertiesRepository::class.java)
        assertSame(repo, compositeOf(listOf(repo)))
        assertSame(repo, compositeOf(emptyList(), repo))
    }

    private fun propsMap(vararg props: Property): Map<String, Property> {
        return stream(props).collect(toMap({ it.key }, identity()))
    }

    private fun second(): Map<String, Property> {
        val map: HashMap<String, Property> = HashMap()
        map.put("foo.bar", two)
        map.put("foo.baz", three)
        return map
    }
}
