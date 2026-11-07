package io.microconfig.core

import io.microconfig.core.properties.ResolveException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class WildcardPlaceholdersTest {
    @TempDir lateinit var root: Path

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun resolveGroupsInDefaultsAndLists(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/app/application.yaml",
            "whole: \${source@db.*}\n" +
                "fallback: \${missing@value:\${source@db.*}}\n" +
                "list: [\${source@db.*}]\n" +
                "defaultList: \${missing@value:[\${source@db.*}]}\n" +
                "empty: \${source@absent.*:{}}\nsibling: tail\n")
        val result = build()
        val group = mapOf("host" to "localhost", "auth" to mapOf("user" to "reader"),
            "computed" to 3, "text" to "a: b")
        assertEquals(group, result["whole"])
        assertEquals(group, result["fallback"])
        assertEquals(listOf(group), result["list"])
        assertEquals(listOf(group), result["defaultList"])
        assertEquals(emptyMap<String, Any>(), result["empty"])
        assertEquals("tail", result["sibling"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun rejectMissingGroupWithoutDefault(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/app/application.yaml", "value: \${source@absent.*}\n")
        assertThrows(ResolveException::class.java) { build() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepExplicitEnvironmentAndCacheSeparate(reverse: Boolean) {
        writeRepository(true)
        write("envs/prod.yaml", "{}\n")
        write("components/source/application.yaml",
            "db:\n  host: \${this@host}\n  items: \${this@items}\n")
        write("components/source/application.dev.yaml", "host: dev\nitems: [1]\n")
        write("components/source/application.prod.yaml", "host: prod\nitems: [2]\n")
        val references = listOf("implicit: \${source@db.*}\n", "explicit: \${source[prod]@db.*}\n")
        write("components/app/application.yaml",
            (if (reverse) references.reversed() else references).joinToString(""))
        val result = build()
        assertEquals(mapOf("host" to "dev", "items" to listOf(1)), result["implicit"])
        assertEquals(mapOf("host" to "prod", "items" to listOf(2)), result["explicit"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun keepConfigTypesSeparate(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/source/process.process", if (yamlSource)
            "db:\n  host: process-host\n  value: #{2 + 3}\n"
        else "db.host=process-host\ndb.value=#{2 + 3}\n")
        write("components/app/application.yaml",
            "application: \${source@db.*}\nprocess: \${process::source@db.*}\nagain: \${source@db.*}\n")
        val result = build()
        assertEquals("localhost", (result["application"] as Map<*, *>)["host"])
        assertEquals(mapOf("host" to "process-host", "value" to 5), result["process"])
        assertEquals(result["application"], result["again"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun resolveDynamicAndNestedPrefixesWithoutEvaluatingUnusedDefault(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/app/application.yaml",
            "dynamic: \${source@\${missing@prefix:db}.*}\n" +
                "nested: \${source@db.auth.*}\n" +
                "unused: \${source@db.*:#{1 / 0}}\n")
        val result = build()
        assertEquals("localhost", (result["dynamic"] as Map<*, *>)["host"])
        assertEquals(mapOf("user" to "reader"), result["nested"])
        assertEquals(result["dynamic"], result["unused"])
    }

    @Test
    fun preserveCollectionsNullAndQuotedKeysInGroup() {
        writeRepository(true)
        write("components/source/application.yaml",
            "db:\n  items: [one, two]\n  empty: []\n  nullValue: null\n  flag: true\n" +
                "  quotedNumber: '123'\n  number: 123\n  'a.b': 'a: b'\n")
        write("components/app/application.yaml", "group: \${source@db.*}\n")
        assertEquals(mapOf("items" to listOf("one", "two"), "empty" to emptyList<Any>(),
            "nullValue" to null, "flag" to true, "quotedNumber" to "123", "number" to 123,
            "a.b" to "a: b"), build()["group"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun detectCyclesThroughGroupChildren(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/source/application${if (yamlSource) ".yaml" else ".properties"}",
            if (yamlSource) "db:\n  loop: \${source@db.*}\n" else "db.loop=\${source@db.*}\n")
        write("components/app/application.yaml", "group: \${source@db.*}\n")
        assertTimeoutPreemptively(Duration.ofSeconds(3)) {
            assertThrows(ResolveException::class.java) { build() }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["*", "db*", "db.*.host"])
    fun useDefaultForUnsupportedWildcardForms(key: String) {
        writeRepository(true)
        write("components/app/application.yaml", "value: \${source@$key:fallback}\n")
        assertEquals("fallback", build()["value"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun preserveGroupIndentationAtFifthLevel(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/app/application.yaml",
            "one:\n  two:\n    three:\n      four:\n        before: kept\n" +
                "        settings: \${source@db.*}\n        after: kept\n")
        assertFifthLevelStructure(buildText())
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun preserveTemplateGroupIndentationAtFifthLevel(yamlSource: Boolean) {
        writeRepository(yamlSource)
        write("components/app/application.yaml",
            "mc.template.deep: \${this@configDir}/source.yaml -> rendered.yaml\n")
        write("components/app/source.yaml",
            "one:\n  two:\n    three:\n      four:\n        before: kept\n" +
                "        settings:\n          \${source@db.*}\n        after: kept\n")
        val output = buildOutput()
        assertFifthLevelStructure(Files.readString(output.resolve("app/rendered.yaml")))
    }

    private fun writeRepository(yamlSource: Boolean) {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        if (yamlSource) write("components/source/application.yaml",
            "db:\n  host: localhost\n  auth:\n    user: \${missing@user:reader}\n" +
                "  computed: #{1 + 2}\n  text: 'a: b'\ndbOther:\n  leak: excluded\n")
        else write("components/source/application.properties",
            "db.host=localhost\ndb.auth.user=\${missing@user:reader}\n" +
                "db.computed=#{1 + 2}\ndb.text=a: b\ndbOther.leak=excluded\n")
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun build(): Map<String, Any?> = Yaml().load(buildText())

    private fun buildText(): String =
        Files.readString(buildOutput().resolve("app/application.yaml"))

    private fun buildOutput(): Path {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return output
    }

    private fun assertFifthLevelStructure(text: String) {
        val group = mapOf("host" to "localhost", "auth" to mapOf("user" to "reader"),
            "computed" to 3, "text" to "a: b")
        val branch = mapOf("before" to "kept", "settings" to group, "after" to "kept")
        val expected = mapOf("one" to mapOf("two" to mapOf("three" to mapOf("four" to branch))))
        assertEquals(expected, Yaml().load<Map<String, Any?>>(text))
        assertTrue("        settings:" in text.lines(), text)
        assertTrue("          host: localhost" in text.lines(), text)
        assertTrue("          auth:" in text.lines(), text)
        assertTrue("            user: reader" in text.lines(), text)
    }
}
