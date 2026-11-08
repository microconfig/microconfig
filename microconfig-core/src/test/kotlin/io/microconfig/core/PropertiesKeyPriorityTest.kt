package io.microconfig.core

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.PropertiesRepository
import io.microconfig.core.properties.PropertyImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class PropertiesKeyPriorityTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun useEnvironmentOverrideAcrossDifferentEncodedKeySpellings(encodedBase: Boolean) {
        val base = if (encodedBase) "a\\u002eb" else "a.b"
        val override = if (encodedBase) "a.b" else "a\\u002eb"
        write(
            "components/app/application.properties", "$base=base\ncopy=\${this@a.b}\n" +
                    "mc.mustache.t=\${this@configDir}/template.txt -> rendered.txt\n"
        )
        write("components/app/application.dev.properties", "$override=override\n")
        write("components/app/template.txt", "{{a.b}}|\${this@a.b}")

        assertResolvedValue("override", build())
        assertEquals("override|override", Files.readString(root.resolve("output/app/rendered.txt")))
        assertSingleRawKey(override, setOf(base, override))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = [
            "a.b|a\\u002eb", "a\\u002eb|\\u0061.b", "\\u0061.b|a.b",
        ]
    )
    fun useLastEncodedAliasWithinOneFile(first: String, last: String) {
        write("components/app/application.properties", "$first=first\n$last=last\ncopy=\${this@a.b}\n")

        assertResolvedValue("last", build())
        assertSingleRawKey(last, setOf(first, last))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun applyProfilesAndEnvironmentInPriorityOrder(withEnvironmentOverride: Boolean) {
        write("envs/dev.yaml", "profiles: [p1, p2]\ngroup:\n  components: [app]\n")
        write("components/app/application.properties", "a.b=base\ncopy=\${this@a.b}\n")
        write("components/app/application.p1.properties", "a\\u002eb=first-profile\n")
        write("components/app/application.p2.properties", "\\u0061.b=second-profile\n")
        if (withEnvironmentOverride)
            write("components/app/application.dev.properties", "\\u0061.\\u0062=environment\n")

        assertResolvedValue(if (withEnvironmentOverride) "environment" else "second-profile", build())
    }

    @ParameterizedTest
    @CsvSource(value = ["false,false", "true,false", "false,true", "true,true"])
    fun keepLocalAndIncludeOrderPriorities(reverseIncludes: Boolean, withLocalValue: Boolean) {
        val includes = if (reverseIncludes) "second, first" else "first, second"
        write("components/first/application.properties", "a\\u002eb=first\n")
        write("components/second/application.properties", "\\u0061.b=second\n")
        write(
            "components/app/application.properties", "#include $includes\n" +
                    (if (withLocalValue) "a.b=local\n" else "") + "copy=\${this@a.b}\n"
        )

        val expected = if (withLocalValue) "local" else if (reverseIncludes) "first" else "second"
        assertResolvedValue(expected, build())
    }

    @Test
    fun keepFirstRepositoryPriorityAfterSelectingItsLastEncodedAlias() {
        val first = PropertyImpl.property("a.b", "older-custom", PROPERTIES, null)
        val preferred = PropertyImpl.property("a\\u002eb", "preferred-custom", PROPERTIES, null)
        val lower = PropertyImpl.property("\\u0061.b", "lower-custom", PROPERTIES, null)
        val repositories = listOf(
            PropertiesRepository { _, _, _ -> linkedMapOf(first.key to first, preferred.key to preferred) },
            PropertiesRepository { _, _, _ -> mapOf(lower.key to lower) },
        )
        write("components/app/application.properties", "a.b=file\ncopy=\${this@a.b}\n")
        val microconfig = microconfig().withAdditionalPropertiesRepositories(repositories)
        val properties = microconfig.dependencies().propertiesFactory()
            .getPropertiesOf("app", "app", "dev", listOf(APPLICATION)).first()

        assertSame(preferred, properties.getPropertyWithKey("a.b").orElseThrow())
        assertEquals(setOf(preferred.key, "copy"), properties.propertiesAsMap.keys)
        assertResolvedValue("preferred-custom", build(microconfig))
    }

    @Test
    fun retainYamlTypedKeysAndLiteralDottedPathsAlongsidePropertiesKeys() {
        write("components/source/application.properties", "\\u0031=string\na\\u002eb=nested\n")
        write(
            "components/app/application.yaml", "#include source\n1: number\n\"a.b\": literal\n" +
                    "copy: \${source@a.b}\n"
        )

        MicroconfigRunner(microconfig()).build("dev", emptyList(), emptyList())
        val output = Files.readString(root.resolve("output/app/application.yaml"))
        assertEquals(
            mapOf(
                1 to "number", "1" to "string", "a.b" to "literal",
                "a" to mapOf("b" to "nested"), "copy" to "nested"
            ), Yaml().load<Map<*, *>>(output)
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun retainLocalPriorityWhenMixedFormatsUseTheSameRawKey(yaml: Boolean) {
        write("components/first/application.properties", "1=first\n")
        write("components/second/application.yaml", "1: second\n")
        val extension = if (yaml) "yaml" else "properties"
        val separator = if (yaml) ": " else "="
        write(
            "components/app/application.$extension", "#include first, second\n" +
                    "1${separator}local\ncopy${separator}\${this@1}\n"
        )

        MicroconfigRunner(microconfig()).build("dev", emptyList(), emptyList())
        val output = Files.readString(root.resolve("output/app/application.$extension"))

        if (yaml) assertEquals(mapOf(1 to "local", "copy" to "local"), Yaml().load<Map<*, *>>(output))
        else assertEquals(
            mapOf("1" to "local", "copy" to "local"),
            Properties().apply { load(StringReader(output)) })
    }

    @Test
    fun useTheSameLogicalWinnerInWildcardGroupsAndDirectReferences() {
        write("components/source/application.properties", "db.value=base\ndb.va\\u006cue=override\n")
        write("components/app/application.properties", "copy=\${source@db.value}\ngroup=\${source@db.*}\n")

        val result = build()

        assertEquals("override", result.getProperty("copy"))
        assertEquals(mapOf("value" to "override"), Yaml().load<Map<*, *>>(result.getProperty("group")))
    }

    @ParameterizedTest
    @ValueSource(strings = ["a\\u002eb", "\\u0061.b"])
    fun keepEncodedQueriesWorkingAfterTheirRawSpellingIsOverridden(encoded: String) {
        write("components/source/application.properties", "$encoded.value=base\n")
        write("components/source/application.dev.properties", "a.b.value=override\n")
        write(
            "components/app/application.yaml", "copy: \${source@$encoded.value:fallback}\n" +
                    "group: \${source@$encoded.*:fallback}\n"
        )

        MicroconfigRunner(microconfig()).build("dev", emptyList(), emptyList())
        val output = Files.readString(root.resolve("output/app/application.yaml"))

        assertEquals(
            mapOf("copy" to "override", "group" to mapOf("value" to "override")),
            Yaml().load<Map<*, *>>(output)
        )
    }

    @Test
    fun keepLocalAndIncludedInsertionOrderWhenEnvironmentOverridesExistingKeys() {
        write("components/source/application.properties", "included=include\nshared=include\n")
        write("components/app/application.properties", "#include source\nlocal=local\nshared=base\nlast=last\n")
        write("components/app/application.dev.properties", "shared=override\nadded=environment\n")

        build()

        assertEquals(
            listOf(
                "local=local", "shared=override", "last=last", "included=include",
                "added=environment"
            ), Files.readAllLines(root.resolve("output/app/application.properties"))
        )
    }

    @Test
    fun keepHigherRepositoryEntriesBeforeNonConflictingLowerPriorityEntries() {
        val preferred = PropertyImpl.property("preferred", "custom", PROPERTIES, null)
        val shared = PropertyImpl.property("shared", "custom", PROPERTIES, null)
        val repository = PropertiesRepository { _, _, _ ->
            linkedMapOf(preferred.key to preferred, shared.key to shared)
        }
        write("components/app/application.properties", "local=file\nshared=file\n")

        build(microconfig().withAdditionalPropertiesRepositories(listOf(repository)))

        assertEquals(
            listOf("preferred=custom", "shared=custom", "local=file"),
            Files.readAllLines(root.resolve("output/app/application.properties"))
        )
    }

    @Test
    fun removeLogicalPropertyWhenTemporaryAliasWins() {
        write("components/app/application.properties", "a.b=base\n#var a\\u002eb=\ncopy=\${this@a.b}\n")

        val result = build()

        assertFalse(result.containsKey("a.b"))
        assertEquals("", result.getProperty("copy"))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = [
            "#var|a\\:b|a\\\\u003ab", ".var|a\\:b|a\\\\u003ab",
            "#var|a\\=b|a\\\\u003db", ".var|a\\=b|a\\\\u003db",
        ]
    )
    fun hideIncludedPropertiesWithEscapedTemporaryKeys(directive: String, key: String, query: String) {
        write("components/source/application.properties", "$key=secret\nretained=stable\n")
        write(
            "components/app/application.properties",
            "#include source\n$directive $key=temporary\ncopy=\${this@$query}\n",
        )

        val result = build()

        assertEquals(mapOf("retained" to "stable", "copy" to "temporary"), result)
    }

    @Test
    fun keepRejectingIdenticalRawKeysWithinOneFile() {
        write("components/app/application.properties", "a.b=first\na.b=second\n")

        val failure = assertThrows(IllegalStateException::class.java) { build() }

        assertTrue(failure.message.orEmpty().contains("declared twice"), failure.message)
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun microconfig(): Microconfig =
        Microconfig.searchConfigsIn(root.toFile()).withDestinationDir(root.resolve("output").toFile())

    private fun build(microconfig: Microconfig = microconfig()): Properties {
        MicroconfigRunner(microconfig).build("dev", emptyList(), emptyList())
        return Properties().apply {
            load(StringReader(Files.readString(root.resolve("output/app/application.properties"))))
        }
    }

    private fun assertResolvedValue(expected: String, properties: Properties) {
        assertEquals(expected, properties.getProperty("a.b"))
        assertEquals(expected, properties.getProperty("copy"))
    }

    private fun assertSingleRawKey(expected: String, spellings: Set<String>) {
        val lines = Files.readAllLines(root.resolve("output/app/application.properties"))
        assertEquals(listOf(expected), lines.map { it.substringBefore('=') }.filter { it in spellings })
    }
}
