package io.microconfig.core.properties

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.utils.FileUtils.LINES_SEPARATOR
import java.util.*
import java.util.Optional.empty
import java.util.Optional.of
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TypedPropertiesImplTest {
    val p1: Property = property("key")

    val p2: Property = property("var")

    val subj: TypedProperties = withProperties(listOf(p1, p2))

    @BeforeEach
    fun setup() {
        `when`(p1.isVar).thenReturn(false)
        `when`(p2.isVar).thenReturn(true)
    }

    @Test
    fun getDeclaringComponent() {
        assertEquals(DeclaringComponentImpl("app", "comp", "env"), subj.declaringComponent)
    }

    @Test
    fun resolve() {
        val resolved1: Property = property("resolved1")
        val resolved2: Property = property("resolved2")
        val resolver: Resolver = mock(Resolver::class.java)
        val root: DeclaringComponent = DeclaringComponentImpl(APPLICATION.getName(), "comp", "env")
        `when`(p1.resolveBy(resolver, root)).thenReturn(resolved1)
        `when`(p2.resolveBy(resolver, root)).thenReturn(resolved2)
        assertEquals(withProperties(listOf(resolved1, resolved2)), subj.resolveBy(resolver))
    }

    @Test
    fun withoutTempValues() {
        assertEquals(withProperties(listOf(p1)), subj.withoutVars())
    }

    @Test
    fun propertiesAsMap() {
        val expected = HashMap<String, Property>()
        expected.put("key", p1)
        expected.put("var", p2)
        assertEquals(expected, subj.propertiesAsMap)
    }

    @Test
    fun propertiesAsKeyValue() {
        assertEquals(mapOf("key" to "keyValue", "var" to "varValue"), subj.propertiesAsKeyValue)
    }

    @Test
    fun properties() {
        assertEquals(listOf(p1, p2), ArrayList(subj.properties))
    }

    @Test
    fun propertyWithKey() {
        assertEquals(of(p1), subj.getPropertyWithKey("key"))
        assertEquals(empty<Any>(), subj.getPropertyWithKey("missing"))
    }

    @Test
    fun getMultipleProperties() {
        val tp: TypedProperties =
            withProperties(listOf(property("k1.k2"), property("k1.k3"), property("k4")))
        assertEquals(
            "k2: k1.k2Value" + LINES_SEPARATOR + "k3: k1.k3Value",
            tp.getPropertyWithKey("k1.*").get().value,
        )
        assertEquals(Optional.empty<Any>(), tp.getPropertyWithKey("missing.*"))
    }

    @Test
    fun getMultiplePropertiesExcludesSimilarPrefixes() {
        val tp: TypedProperties = withProperties(listOf(property("foo.port"), property("foobar")))
        assertEquals("port: foo.portValue", tp.getPropertyWithKey("foo.*").orElseThrow().value)
    }

    @Test
    fun getMultiplePropertiesIgnoresParentKey() {
        val tp: TypedProperties = withProperties(listOf(property("foo"), property("foo.port")))
        assertEquals("port: foo.portValue", tp.getPropertyWithKey("foo.*").orElseThrow().value)
    }

    @Test
    fun getMultiplePropertiesReturnsEmptyWithoutChildKeys() {
        val tp: TypedProperties = withProperties(listOf(property("foo"), property("foobar")))
        assertEquals(Optional.empty<Any>(), tp.getPropertyWithKey("foo.*"))
    }

    @Test
    fun withPrefixMatchesTextPrefixes() {
        val properties: List<Property> =
            listOf(property("foo"), property("foo.port"), property("foobar"))
        val tp: TypedProperties = withProperties(properties)
        assertEquals(properties, ArrayList(tp.withPrefix("foo").properties))
    }

    @Test
    fun serialize() {
        val serializer: PropertySerializer<String> = { p, t, ct, c, e -> c }
        assertEquals("comp", subj.save(serializer))
    }

    @Test
    fun testToString() {
        assertEquals("comp[env]", subj.toString())
    }

    private fun withProperties(properties: Collection<Property>): TypedProperties {
        return TypedPropertiesImpl(
            APPLICATION,
            "comp",
            "env",
            properties.associateBy { it.key },
            emptyList(),
        )
    }

    private fun property(key: String): Property {
        val property: Property = mock(Property::class.java)
        `when`(property.key).thenReturn(key)
        `when`(property.value).thenReturn(key + "Value")
        return property
    }
}
