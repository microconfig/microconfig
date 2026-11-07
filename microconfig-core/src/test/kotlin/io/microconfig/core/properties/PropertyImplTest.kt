package io.microconfig.core.properties

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.PropertyImpl.Companion.findSeparatorIndexIn
import io.microconfig.core.properties.PropertyImpl.Companion.isComment
import io.microconfig.core.properties.PropertyImpl.Companion.isTempProperty
import io.microconfig.core.properties.PropertyImpl.Companion.parse
import io.microconfig.core.properties.PropertyImpl.Companion.property
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

class PropertyImplTest {
    val yamlSource: DeclaringComponent = fileSource(File("comp/config.yaml"), 0, true, "app", "dev")

    val yaml: Property = parse("key: value", PROPERTIES, yamlSource)

    val tempYaml: Property = parse("#var var: value", PROPERTIES, yamlSource)

    val propSource: DeclaringComponent =
        fileSource(File("comp/config.properties"), 0, false, "app", "dev")

    val prop: Property = parse("key=value", PROPERTIES, propSource)

    val tempProp: Property = parse("#var var=value", PROPERTIES, propSource)

    @Test
    fun parseYaml() {
        assertEquals("key", yaml.key)
        assertEquals("value", yaml.value)
        assertEquals(yamlSource, yaml.declaringComponent)
        assertFalse(yaml.isVar)
        assertEquals("var", tempYaml.key)
        assertEquals("value", tempYaml.value)
        assertEquals(yamlSource, tempYaml.declaringComponent)
        assertTrue(tempYaml.isVar)
    }

    @Test
    fun parseProp() {
        assertEquals("key", prop.key)
        assertEquals("value", prop.value)
        assertEquals(propSource, prop.declaringComponent)
        assertFalse(prop.isVar)
        assertEquals("var", tempProp.key)
        assertEquals("value", tempProp.value)
        assertEquals(propSource, tempProp.declaringComponent)
        assertTrue(tempProp.isVar)
    }

    @Test
    fun failOnMissingSeparator() {
        assertThrows(
            IllegalArgumentException::class.java,
            { parse("key value", PROPERTIES, yamlSource) },
        )
    }

    @Test
    fun factoryMethods() {
        assertEquals(yaml, property("key", "value", PROPERTIES, yamlSource))
    }

    @Test
    fun helperMethods() {
        assertEquals(3, findSeparatorIndexIn("key: value"))
        assertEquals(8, findSeparatorIndexIn("keyValue=value"))
        assertEquals(-1, findSeparatorIndexIn("key value"))
        assertTrue(isComment("# comment"))
        assertFalse(isComment("comment"))
        assertTrue(isTempProperty("#var bla bla"))
        assertFalse(isTempProperty("var bla"))
    }

    @Test
    fun string() {
        assertEquals("key=value", yaml.toString())
        assertEquals("#var=value", tempProp.toString())
    }

    @Test
    fun resolve() {
        val resolved: String = "resolved"
        val resolver: Resolver = mock(Resolver::class.java)
        `when`(resolver.resolve(yaml, propSource)).thenReturn(resolved)
        assertEquals(
            property("key", resolved, PROPERTIES, yamlSource),
            yaml.resolveBy(resolver, propSource),
        )
    }
}
