package io.microconfig.core

import io.microconfig.core.properties.ResolveException
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WildcardUseCasesTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest(name = "source YAML={0}, target YAML={1}")
    @MethodSource("formats")
    fun transferGroupsAcrossConfigFormats(sourceYaml: Boolean, targetYaml: Boolean) {
        writeSource(sourceYaml)
        write("components/app/application${if (targetYaml) ".yaml" else ".properties"}",
            "value${if (targetYaml) ": " else "="}\${source@db.*}\n")
        val output = buildOutput()
        val actual = if (targetYaml) readYaml(output.resolve("app/application.yaml"))["value"]
            else Yaml().load<Any>(Properties().apply {
                load(StringReader(Files.readString(output.resolve("app/application.properties"))))
            }.getProperty("value"))
        assertEquals(database(), actual)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun composeMultipleGroupsAndRepeatReferences(sourceYaml: Boolean) {
        writeSource(sourceYaml)
        write("components/app/application.yaml",
            "groups: [\${source@db.*}, \${source@http.*}, \${source@db.*}]\n" +
                "fallback: \${missing@value:[\${source@db.*}, \${source@absent.*:{}}, \${source@http.*}]}\n")
        val result = buildYaml()
        assertEquals(listOf(database(), mapOf("port" to 8080), database()), result["groups"])
        assertEquals(listOf(database(), emptyMap<String, Any>(), mapOf("port" to 8080)), result["fallback"])
    }

    @ParameterizedTest
    @MethodSource("dynamicReferences")
    fun resolveComputedReferenceHeaders(expression: String, host: String) {
        writeSource(true)
        write("envs/prod.yaml", "{}\n")
        write("components/source/application.prod.yaml", "db:\n  host: prod-host\n")
        write("components/app/application.yaml", "value: $expression\nsibling: kept\n")
        val result = buildYaml()
        assertEquals(database() + ("host" to host), result["value"])
        assertEquals("kept", result["sibling"])
    }

    @ParameterizedTest
    @MethodSource("canonicalPrefixes")
    fun resolveGroupsUnderEquivalentYamlKeys(prefix: String, expected: String) {
        write("components/source/application.yaml",
            "values:\n  true:\n    leaf: boolean\n  16:\n    leaf: integer\n" +
                "  1.5:\n    leaf: float\n  1:\n    leaf: one\n")
        write("components/app/application.yaml", "value: \${source@$prefix.*}\n")
        assertEquals(mapOf("leaf" to expected), buildYaml()["value"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun keepRootOverridesSeparateWhileSharingResolver(reverse: Boolean) {
        write("envs/dev.yaml", "group:\n  components: ${if (reverse) "[second, first]" else "[first, second]"}\n")
        write("components/source/application.yaml", "db:\n  label: \${this@label}\n")
        write("components/first/application.yaml", "label: first\nvalue: \${source@db.*}\n")
        write("components/second/application.yaml", "label: second\nvalue: \${source@db.*}\n")
        val output = buildOutput()
        assertEquals(mapOf("label" to "first"), readYaml(output.resolve("first/application.yaml"))["value"])
        assertEquals(mapOf("label" to "second"), readYaml(output.resolve("second/application.yaml"))["value"])
    }

    @Test
    fun selectInheritedAndProfiledChildrenAfterRootOverrides() {
        writeSource(true)
        write("envs/dev.yaml", "profiles: [tuned]\ngroup:\n  components: [app]\n")
        write("components/source/application.tuned.yaml", "db:\n  port: 7000\n")
        write("components/app/application.yaml", "#include source\ndb:\n  host: root-host\nselected: \${this@db.*}\n")
        assertEquals(database() + mapOf("host" to "root-host", "port" to 7000), buildYaml()["selected"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun preserveGroupsInsideQuotedTextAndSpel(sourceYaml: Boolean) {
        writeSource(sourceYaml)
        write("components/app/application.yaml",
            "text: \"prefix \${source@db.*} suffix\"\n" +
                "expression: #{'\${source@db.*}'.contains('localhost')}\n")
        val result = buildYaml()
        val fragment = (result["text"] as String).removePrefix("prefix ").removeSuffix(" suffix")
        assertEquals(database(), Yaml().load<Map<*, *>>(fragment))
        assertEquals(true, result["expression"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun preserveEscapedGroupAndResolveFollowingTemplateReference(sourceYaml: Boolean) {
        writeSource(sourceYaml)
        write("components/app/application.yaml", "mc.template.t: \${this@configDir}/template.yaml -> rendered.yaml\n")
        write("components/app/template.yaml",
            "literal: \\\${source@db.*}\nresolved:\n  \${source@db.*}\nsibling: kept\n")
        val result = readYaml(buildOutput().resolve("app/rendered.yaml"))
        assertEquals("\${source@db.*}", result["literal"])
        assertEquals(database(), result["resolved"])
        assertEquals("kept", result["sibling"])
    }

    @Test
    fun resolveGroupsWhoseChildrenReferenceOtherGroups() {
        write("components/cluster/application.yaml", "service:\n  hosts: [one, two]\n  empty: []\n")
        write("components/source/application.yaml", "db:\n  cluster: \${cluster@service.*}\n  size: #{1 + 2}\n")
        write("components/app/application.yaml", "value: \${missing@value:[\${source@db.*}]}\n")
        assertEquals(listOf(mapOf("cluster" to mapOf("hosts" to listOf("one", "two"),
            "empty" to emptyList<Any>()), "size" to 3)), buildYaml()["value"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun rejectBrokenGroupChildInsteadOfUsingFallback(sourceYaml: Boolean) {
        write("components/source/application${if (sourceYaml) ".yaml" else ".properties"}",
            if (sourceYaml) "db:\n  value: #{1 / 0}\n" else "db.value=#{1 / 0}\n")
        write("components/app/application.yaml", "value: \${source@db.*:{fallback: true}}\n")
        assertThrows(ResolveException::class.java) { buildYaml() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun useWholeGroupDefaultWhenAChildIsUnresolved(sourceYaml: Boolean) {
        write("components/source/application${if (sourceYaml) ".yaml" else ".properties"}",
            if (sourceYaml) "db:\n  kept: value\n  missing: \${absent@value}\n"
            else "db.kept=value\ndb.missing=\${absent@value}\n")
        write("components/app/application.yaml", "value: \${source@db.*:{fallback: true}}\n")
        assertEquals(mapOf("fallback" to true), buildYaml()["value"])
    }

    @Test
    fun selectPropertiesChildrenByDecodedPrefixWithoutDoubleDecodingKeys() {
        write("components/source/application.properties",
            "db\\u002ehost=localhost\ndb.port=5432\nd\\u0062.auth.user=reader\n" +
                "d\\u0062.literal\\\\u0041=payload\ndbOther.leak=excluded\n")
        write("components/app/application.yaml", "value: \${source@db.*}\n")
        assertEquals(mapOf("host" to "localhost", "port" to 5432,
            "auth" to mapOf("user" to "reader"), "literal\\u0041" to "payload"), buildYaml()["value"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["db.", "db\\u002e", "d\\u0062."])
    fun preserveEscapesAndControlCharactersInChildKeys(prefix: String) {
        write("components/source/application.properties",
            "${prefix}literal\\\\u0041=one\n${prefix}line\\r\\nname=two\n" +
                "${prefix}db\\:url=three\n${prefix}tab\\tname=four\n")
        write("components/app/application.yaml", "value: \${source@db.*}\n")
        assertEquals(mapOf("literal\\u0041" to "one", "line\r\nname" to "two",
            "db:url" to "three", "tab\tname" to "four"), buildYaml()["value"])
    }

    @Test
    fun preserveGroupWithScalarParentAndSimilarPrefixes() {
        write("components/source/application.properties", "db=parent\ndb.host=localhost\ndbExtra.host=excluded\n")
        write("components/app/application.yaml", "value: \${source@db.*}\n")
        assertEquals(mapOf("host" to "localhost"), buildYaml()["value"])
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun writeSource(yaml: Boolean) {
        if (yaml) write("components/source/application.yaml",
            "db:\n  host: localhost\n  port: 5432\n  auth:\n    user: reader\n" +
                "  label: O'Reilly \"book\"\nhttp:\n  port: 8080\n")
        else write("components/source/application.properties",
            "db.host=localhost\ndb.port=5432\ndb.auth.user=reader\ndb.label=O'Reilly \"book\"\nhttp.port=8080\n")
    }

    private fun buildOutput(): Path {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return output
    }

    private fun readYaml(file: Path): Map<String, Any?> = Yaml().load(Files.readString(file))

    private fun buildYaml(): Map<String, Any?> = readYaml(buildOutput().resolve("app/application.yaml"))

    private fun database(): Map<String, Any> = mapOf("host" to "localhost", "port" to 5432,
        "auth" to mapOf("user" to "reader"), "label" to "O'Reilly \"book\"")

    private fun formats(): List<Arguments> = listOf(true, false).flatMap { source ->
        listOf(true, false).map { target -> Arguments.of(source, target) }
    }

    private fun dynamicReferences(): List<Arguments> = listOf(
        Arguments.of("\${source@\${missing@prefix:db}.*}", "localhost"),
        Arguments.of("\${\${missing@component:source}@db.*}", "localhost"),
        Arguments.of("\${\${missing@type:app}::\${missing@component:source}[\${missing@targetEnv:prod}]@\${missing@prefix:db}.*}", "prod-host"),
    )

    private fun canonicalPrefixes(): List<Arguments> = listOf(
        Arguments.of("values.yes", "boolean"), Arguments.of("values.true", "boolean"),
        Arguments.of("values.YES", "boolean"), Arguments.of("values.0x10", "integer"),
        Arguments.of("values.16", "integer"), Arguments.of("values.1.50", "float"),
        Arguments.of("values.1.5", "float"), Arguments.of("values.01", "one"), Arguments.of("values.1", "one"),
    )
}
