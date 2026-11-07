package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class YamlResolutionBoundariesTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "{port: 80, port: 81}",
        "[{port: 80, port: 81}]",
        "{nested: {yes: first, true: second}}",
        "{nested: {1_0: first, 10: second}}",
        "{nested: {1.00: first, 1.0: second}}",
        "{nested: {~: first, null: second}}",
    ])
    fun rejectStaticMappingDuplicatesBeforeWriting(value: String) {
        write("components/app/application.yaml", "result: $value\n")
        write("output/app/application.yaml", "retained: true\n")

        val error = assertThrows(RuntimeException::class.java) { build() }

        assertTrue(error.message.orEmpty().contains("Duplicate YAML key"), error.message)
        assertEquals("retained: true\n", Files.readString(root.resolve("output/app/application.yaml")))
    }

    @Test
    fun retainDistinctStaticKeyTypesAndYamlMergeOverrides() {
        write("components/app/application.yaml", """
            result: {1: number, '1': text, true: boolean, 'true': text-boolean}
            merged: {base: &base {port: 80}, copy: {<<: *base, port: 81}}
            """.trimIndent())

        val result = build()

        assertEquals(mapOf(1 to "number", "1" to "text", true to "boolean", "true" to "text-boolean"), result["result"])
        assertEquals(mapOf("base" to mapOf("port" to 80), "copy" to mapOf("port" to 81)), result["merged"])
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value = [
            "name | name",
            "name | 'name'",
            "'name' | name",
            "1_0 | 10",
            "0xA | 10",
            "!!int '010' | 8",
            "YES | true",
            "~ | null",
            "1.00 | 1.0",
        ],
    )
    fun rejectEquivalentMappingKeysAfterResolution(value: String, otherKey: String) {
        write("components/source/application.yaml", "key: $value\n")
        write("components/app/application.yaml", "result: {\${source@key}: first, $otherKey: second}\n")

        val error = assertThrows(RuntimeException::class.java) { build() }

        assertTrue(generateSequence(error as Throwable?) { it.cause }
            .any { it.message?.contains("Duplicate YAML key") == true }, error.toString())
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        quoteCharacter = '"',
        value = ["1 | '1'", "true | 'true'", "null | 'null'", "1 | 1.0"],
    )
    fun keepDifferentYamlKeyTypes(value: String, otherKey: String) {
        write("components/source/application.yaml", "key: $value\n")
        write("components/app/application.yaml", "result: {\${source@key}: first, $otherKey: second}\n")

        val keys = Yaml().load<Map<Any?, Any?>>("{$value: first, $otherKey: second}")
        assertEquals(keys, build()["result"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["true", "YES", "Yes", "on", "ON"])
    fun useYamlBooleanValuesInExpressions(value: String) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{\${source@value} ? 'yes' : 'no'}\n")

        assertEquals("yes", build()["result"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["false", "NO", "No", "off", "OFF"])
    fun useFalseYamlBooleanValuesInExpressions(value: String) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{\${source@value} ? 'yes' : 'no'}\n")

        assertEquals("no", build()["result"])
    }

    @ParameterizedTest
    @ValueSource(strings = ["null", "Null", "NULL", "~", ""])
    fun useYamlNullValuesInExpressions(value: String) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{\${source@value} == null}\n")

        assertEquals(true, build()["result"])
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = ["1_000 | 1001", "0x10 | 17", "010 | 9", "1:00 | 61", "1_000.5 | 1001.5",
            ".5 | 1.5", "1.25e+2 | 126.0", "1:00.5 | 61.5", "!!float '1.5' | 2.5"],
    )
    fun useYamlNumbersInExpressions(value: String, expected: String) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{\${source@value} + 1}\n")

        assertEquals(Yaml().load<Any>(expected), build()["result"])
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"',
        value = ["'YES' | 3", "'null' | 4", "'1_000' | 5", "!!str true | 4",
            "!!timestamp '2001-12-15' | 10", "!!binary 'aGVsbG8=' | 8", "!custom 'abc' | 3"])
    fun keepYamlStringsAsSpelStrings(value: String, length: Int) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{\${source@value}.length()}\n")

        assertEquals(length, build()["result"])
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = [".inf | isInfinite", "-.Inf | isInfinite", ".NaN | isNaN"])
    fun useNonFiniteYamlNumbersInExpressions(value: String, method: String) {
        write("components/source/application.yaml", "value: $value\n")
        write("components/app/application.yaml", "result: #{(\${source@value}).$method()}\n")

        assertEquals(true, build()["result"])
    }

    @Test
    fun preserveQuotedExpressionTextAndPropertiesExpressionFragments() {
        write("components/source/application.yaml", "boolean: YES\nnumber: 1_000\ntext: 'it''s true'\n")
        write("components/properties/application.properties", "fragment=1 + 2\n")
        write("components/app/application.yaml", """
            booleanText: #{'${'$'}{source@boolean}'.toLowerCase()}
            numberText: #{'${'$'}{source@number}'}
            quoted: #{${'$'}{source@text}.toUpperCase()}
            properties: #{${'$'}{properties@fragment} * 2}
            """.trimIndent())

        assertEquals(mapOf("booleanText" to "yes", "numberText" to "1_000",
            "quoted" to "IT'S TRUE", "properties" to 5), build())
    }

    @ParameterizedTest
    @ValueSource(strings = ["base", "id001", "id002"])
    fun keepAliasSharingAndMergePrecedenceWhileResolvingKeys(anchor: String) {
        write("components/source/application.yaml", "key: name\nvalue: first\n")
        write("components/app/application.yaml", """
            result: [{base: &$anchor {name: ${'$'}{source@value}, keep: original}, merged: {<<: *$anchor, name: second}, alias: *$anchor, keyed: &keyed {${'$'}{source@key}: first}, keyedAlias: *keyed}]
            """.trimIndent())

        val result = (build()["result"] as List<*>).single() as Map<*, *>
        assertEquals(mapOf("name" to "first", "keep" to "original"), result["base"], result.toString())
        assertEquals(mapOf("name" to "second", "keep" to "original"), result["merged"])
        assertSame(result["base"], result["alias"])
        assertEquals(mapOf("name" to "first"), result["keyed"])
        assertEquals(result["keyed"], result["keyedAlias"])
    }

    @Test
    fun rejectCollisionsBetweenResolvedMergeKeysAndExplicitKeys() {
        write("components/source/application.yaml", "key: name\n")
        write("components/app/application.yaml", "result: [{base: &base {\${source@key}: first}, merged: {<<: *base, name: second}}]\n")

        val error = assertThrows(RuntimeException::class.java) { build() }

        assertTrue(generateSequence(error as Throwable?) { it.cause }
            .any { it.message?.contains("Duplicate YAML key") == true }, error.toString())
    }

    private fun write(path: String, text: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun build(): Map<*, *> {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return Yaml().load(Files.readString(output.resolve("app/application.yaml")))
    }
}
