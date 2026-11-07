package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.io.properties.PropertiesText.escape
import io.microconfig.core.properties.serializers.PropertySerializers.asString
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.yaml.snakeyaml.Yaml

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NestedPlaceholdersTest {
    @TempDir lateinit var root: Path

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("nestedValues")
    fun resolveNestedValues(format: ConfigFormat, expression: String, expected: String) {
        writeRepository(format, expression)
        val result = build(format)
        assertEquals(expected, result["value"].toString())
        assertEquals("tail", result["sibling"])
    }

    @ParameterizedTest(name = "template from {0}: {1}")
    @MethodSource("nestedValues")
    fun renderNestedValuesInTemplate(format: ConfigFormat, expression: String, expected: String) {
        writeRepository(format, "unused")
        writeTemplate(format, expression)
        assertEquals("$expected|actual", renderTemplate())
    }

    private fun writeRepository(format: ConfigFormat, expression: String) {
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        val component = Files.createDirectories(root.resolve("components/app"))
        val content = if (format == PROPERTIES)
            "present=actual\nvalue=${escape(expression)}\nsibling=tail\n"
        else "present: actual\nvalue: $expression\nsibling: tail\n"
        Files.writeString(component.resolve("application${format.extension()}"), content)
    }

    private fun build(format: ConfigFormat): Map<*, *> {
        val properties = MicroconfigRunner(root.toFile(), root.resolve("output").toFile())
            .buildProperties("dev", emptyList(), emptyList())
        val output = properties.asTypedProperties()
            .single { "value" in it.propertiesAsMap }
            .save(asString())
        return if (format == YAML) Yaml().load(output)
        else Properties().apply { load(StringReader(output)) }
    }

    private fun writeTemplate(format: ConfigFormat, expression: String) {
        val component = root.resolve("components/app")
        Files.writeString(component.resolve("source.txt"),
            "\${this@templateMissing:$expression}|\${this@present}")
        val separator = if (format == PROPERTIES) "=" else ": "
        Files.writeString(component.resolve("application${format.extension()}"),
            "mc.template.t$separator\${this@configDir}/source.txt -> rendered.txt\n", APPEND)
    }

    private fun renderTemplate(): String {
        val output = root.resolve("output")
        MicroconfigRunner(root.toFile(), output.toFile()).build("dev", emptyList(), emptyList())
        return Files.readString(output.resolve("app/rendered.txt"))
    }

    private fun nestedValues(): List<Arguments> {
        val special = "O'Reilly \"book\" } { [] # $ @ : \\tail 🙂"
        val cases = listOf(
            "\${this@missing:\${this@alsoMissing:#{#{1 + 2} * 3}}}" to "9",
            "\${this@missing:#{\${this@number:3} * 2}}" to "6",
            "#{'\${this@missing:'a}b'}'.length()}" to "3",
            "\${this@missing:#{\"\${this@alsoMissing:\"a}b\"}\".length()}}" to "3",
            "\${this@missing:#{ {'a': {'b': 2}}['a']['b'] }}" to "2",
            "\${this@\${this@missingKey:present}:#{'a}b'}}" to "actual",
            "\${\${this@missingComponent:this}@present:fallback}" to "actual",
            "\${this@\${this@missingKey:present}:#{'a}: [b] #c'}}" to "actual",
            "\${this@\${this@missingKey:absent}:#{'a}: [b] #c'}}" to "a}: [b] #c",
            "\${this@\${this@missingKey:absent}:#{'a{b'}}" to "a{b",
            "#{'\${this@\${this@missingKey:present}:'a}b'}'.length()}" to "6",
            "\${this@missing:#{'\${this@\${this@missingKey:absent}:'a}b'}'.length()}}" to "3",
            "\${this@present:\${this@missing:#{1 / 0}}}" to "actual",
            "\${this@missing:http://host:8080/a?x=1&y=2}" to "http://host:8080/a?x=1&y=2",
            "\${this@missing:#{#findGroup('[a-z]{2,3}', '123abc')}}|\${this@present}" to "abc|actual",
            "\${this@missing:#{'${special.replace("'", "''")}'}}" to special,
            "\${this@missing:#{\"a\"\"}b\"}}" to "a\"}b",
            "\${this@missing:#{'literal \${'}}" to "literal \${",
            "\${this@missing:#{'literal \${HOME}'}}" to "literal \${HOME}",
            "\${this@missing:#{'literal \${this@'}}" to "literal \${this@",
            "\${this@missing:#{'literal #{'}}" to "literal #{",
        )
        return listOf(YAML, PROPERTIES).flatMap { format ->
            cases.map { (expression, expected) -> Arguments.of(format, expression, expected) }
        }
    }
}
