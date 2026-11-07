package io.microconfig.core.properties.io.yaml

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.FileBasedComponent
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.io.selector.ConfigIoFactory
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

class YamlConfigIoTest {
    private val io = ConfigIoFactory.newConfigIo(DumpedFsReader())

    @Test
    fun testSimpleYaml() {
        val reader = io.readFrom(classpathFile("configFormats/yaml/parse/simple.yaml"))
        assertEquals(
            mapOf(
                "p0" to "p0v",
                "p1.p2.p3.p4.p5" to "p5v",
                "server.port" to "8080",
                "name" to "",
                "name.name2" to "",
                "displayName" to "dv",
            ),
            reader.propertiesAsMap(),
        )
        val properties = reader.properties("", "")
        assertEquals(
            listOf(0, 6, 8),
            properties.take(3).map { (it.declaringComponent as FileBasedComponent).lineNumber },
        )
    }

    @Test
    fun testInnerYaml() {
        assertEquals(
            mapOf(
                "p1.p2.p3.p4.p5" to "p5v",
                "p1.p2.p3.p4.p99" to "",
                "p1.p2.p3.p4.p100" to "p100v",
                "p1.p2.p3_2" to "p3_2v",
                "p1.p2.p3_2.p4_2.p5" to "p5_v",
                "p1.p2.p3_2.p4_2.p6" to "p6_v",
                "p1.p2.p3_2.p35_2" to "p3_2.p35_2_v",
                "p1.p2_2" to "p2_2",
                "p1.p6" to "p6v",
                "p1.p7" to "p7v",
                "p1.p2_3" to "p2_3v",
                "p9" to "p9v",
            ),
            io.readFrom(classpathFile("configFormats/yaml/parse/inner.yaml")).propertiesAsMap(),
        )
    }

    @Test
    fun testInnerYaml2() {
        assertEquals(
            mapOf(
                "cr.cf.tfs.out" to "outV",
                "cr.cf.tfs.out.shouldArchive" to "true",
                "cr.cf.tfs.out.archiveDir" to "dirV",
                "cr2.cf.tfs.out" to "outV",
                "cr2.cf.tfs.out.shouldArchive" to "true",
                "cr2.cf.tfs.out.archiveDir" to "dirV",
            ),
            io.readFrom(classpathFile("configFormats/yaml/parse/inner2.yaml")).propertiesAsMap(),
        )
    }

    @Test
    fun testMultiline() {
        val values =
            io.readFrom(classpathFile("configFormats/yaml/parse/multilines.yaml")).propertiesAsMap()
        assertEquals(
            setOf(
                "management.endpoints",
                "psp.adyen.dodo",
                "psp.adyen.payment-method-list",
                "psp.adyen.value",
                "server.port",
            ),
            values.keys,
        )
        assertEquals("haha", values["psp.adyen.dodo"])
        assertEquals("v2", values["psp.adyen.value"])
        assertEquals("8080", values["server.port"])
        val entries =
            Yaml().load<List<Map<String, Any?>>>(values.getValue("psp.adyen.payment-method-list"))
        assertEquals(listOf("bancontact-card", "bancontact-mobile"), entries.map { it["name"] })
        assertEquals(
            listOf("Bancontact (card)", "Bancontact (mobile)"),
            entries.map { it["displayName"] },
        )
        assertEquals(listOf(0, 0), entries.map { it["fee"] })
        assertEquals(listOf(true, true), entries.map { it["enabled"] })
    }

    @Test
    fun serializeOverlappingTypedAndAnchoredValuesIndependentlyOfInputOrder() {
        val properties = listOf(
            "z.child" to "tail",
            "a.child" to "descendant",
            "a" to "{}",
            "'a.b'" to "literal dot",
            "1" to "typed",
            "'1'" to "string",
            "multiline" to "|+\n  first\n\n",
            "aliases" to "[{base: &id001 {name: first, keep: original}, merged: {<<: *id001, name: second}, alias: *id001}]",
        ).map { (key, value) -> PropertyImpl.property(key, value, YAML, null) }
        val writer = io.writeTo(File("result.yaml"))
        val serialized = writer.serialize(properties)

        assertEquals(serialized, writer.serialize(properties.reversed()))
        assertEquals(serialized, writer.serialize(properties.drop(3) + properties.take(3)))
        val actual = Yaml().load<Map<Any, Any>>(serialized)
        assertEquals(emptyMap<Any, Any>(), actual["a"])
        assertEquals("descendant", actual["a.child"])
        assertEquals("literal dot", actual["a.b"])
        assertEquals("typed", actual[1])
        assertEquals("string", actual["1"])
        assertEquals(mapOf("child" to "tail"), actual["z"])
        assertEquals("first\n\n", actual["multiline"])
        val aliases = (actual["aliases"] as List<*>).single() as Map<*, *>
        assertEquals(mapOf("name" to "first", "keep" to "original"), aliases["base"])
        assertSame(aliases["base"], aliases["alias"])
        assertEquals(mapOf("name" to "second", "keep" to "original"), aliases["merged"])
    }
}
