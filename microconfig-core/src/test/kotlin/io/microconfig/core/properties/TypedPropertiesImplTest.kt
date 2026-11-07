package io.microconfig.core.properties

import io.microconfig.core.MicroconfigRunner
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.impl.TypedPropertiesImpl
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.Optional.empty
import java.util.Optional.of
import java.util.Properties

class TypedPropertiesImplTest {
    @TempDir
    lateinit var directory: Path

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
        val root: DeclaringComponent = DeclaringComponentImpl(APPLICATION.typeName, "comp", "env")
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

    @ParameterizedTest
    @ValueSource(strings = ["a\\u002eb", "a\\ b", "db\\:url", "a\\=b", "\\#name", "literal\\\\u0041"])
    fun findEncodedPropertiesKeysByTheirLogicalNames(key: String) {
        val property = PropertyImpl.property(key, "payload", PROPERTIES, null)
        val properties = withProperties(listOf(property))

        assertEquals(of(property), properties.getPropertyWithKey(PropertiesText.unescape(key)))
        assertEquals(of(property), properties.getPropertyWithKey(key))
        assertEquals(mapOf(key to property), properties.propertiesAsMap)
        assertEquals(
            "$key=payload", configIo().writeTo(directory.resolve("out.properties").toFile())
                .serialize(properties.properties)
        )
    }

    @Test
    fun keepExactAndYamlLookupPriorityOverDecodedPropertiesAliases() {
        val encoded = PropertyImpl.property("a\\u002eb", "encoded", PROPERTIES, null)
        val exact = PropertyImpl.property("a.b", "exact", PROPERTIES, null)
        assertEquals(of(exact), withProperties(listOf(encoded, exact)).getPropertyWithKey("a.b"))

        val encodedNumber = PropertyImpl.property("\\u0031", "encoded", PROPERTIES, null)
        val yamlNumber = PropertyImpl.property("1", "yaml", YAML, null)
        assertEquals(
            of(yamlNumber), withProperties(listOf(encodedNumber, yamlNumber))
                .getPropertyWithKey("01")
        )
    }

    @Test
    fun useLastLogicalPropertiesKeyInDirectConstructorAndCopies() {
        val exact = PropertyImpl.property("a.b", "base", PROPERTIES, null)
        val winner = PropertyImpl.property("a\\u002eb", "override", PROPERTIES, null)
        val properties = withProperties(listOf(exact, winner))

        assertEquals(of(winner), properties.getPropertyWithKey("a.b"))
        assertEquals(mapOf(winner.key to winner), properties.propertiesAsMap)
        assertEquals(emptyList<Property>(), properties.without { it === winner }.properties.toList())
        assertEquals(listOf(winner), properties.withPrefix("a").properties.toList())
        assertEquals(listOf(winner), properties.withPrefix("a.b").properties.toList())
    }

    @Test
    fun decodePropertiesLookupKeysOnlyOnce() {
        val property = PropertyImpl.property("literal\\\\u0041", "payload", PROPERTIES, null)
        val properties = withProperties(listOf(property))
        assertEquals(of(property), properties.getPropertyWithKey("literal\\u0041"))
        assertEquals(empty<Property>(), properties.getPropertyWithKey("literalA"))
    }

    @Test
    fun preferLiteralBackslashLogicalQueriesAndKeepYamlQueriesUndecoded() {
        val literal = PropertyImpl.property("literal\\\\u0041", "literal", PROPERTIES, null)
        val decoded = PropertyImpl.property("literalA", "decoded", PROPERTIES, null)
        val yaml = PropertyImpl.property("1.child", "yaml", YAML, null)
        val properties = withProperties(listOf(literal, decoded, yaml))

        assertEquals(of(literal), properties.getPropertyWithKey("literal\\u0041"))
        assertEquals(empty<Property>(), properties.getPropertyWithKey("\\u0031.child"))
        assertEquals(empty<Property>(), properties.getPropertyWithKey("\\u0031.*"))
    }

    @Test
    fun keepEncodedSuffixesInWildcardGroups() {
        val properties = withProperties(
            listOf(
                PropertyImpl.property("db.url\\:key", "value", PROPERTIES, null),
                PropertyImpl.property("db.literal\\\\u0041", "tail", PROPERTIES, null),
            )
        )
        assertEquals(
            mapOf("url:key" to "value", "literal\\u0041" to "tail"),
            Yaml().load<Map<String, String>>(properties.getPropertyWithKey("db.*").orElseThrow().value)
        )
    }

    @Test
    fun resolveLogicalPropertiesKeysInPlaceholdersAndMustacheTemplates() {
        Files.createDirectories(directory.resolve("envs"))
        Files.writeString(directory.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        val component = Files.createDirectories(directory.resolve("components/app"))
        Files.writeString(
            component.resolve("application.properties"),
            "a\\u002eb=x\ncopy=\${this@a.b:fallback}\n" +
                    "mc.mustache.t=\${this@configDir}/source.txt -> rendered.txt\n"
        )
        Files.writeString(component.resolve("source.txt"), "{{a.b}}|\${this@a.b:fallback}")
        val output = directory.resolve("output")

        MicroconfigRunner(directory.toFile(), output.toFile()).build("dev", emptyList(), emptyList())

        val serialized = Files.readString(output.resolve("app/application.properties"))
        val loaded = Properties().apply { load(StringReader(serialized)) }
        assertEquals("x", loaded["copy"])
        assertEquals("x|x", Files.readString(output.resolve("app/rendered.txt")))
        assertTrue(serialized.contains("a\\u002eb=x"), serialized)
    }

    @Test
    fun getMultipleProperties() {
        val tp: TypedProperties =
            withProperties(listOf(property("k1.k2"), property("k1.k3"), property("k4")))
        assertEquals(
            "k2: k1.k2Value\nk3: k1.k3Value",
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
