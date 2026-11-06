package io.microconfig.core

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml

class YamlFlowValuesTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [target]\n")
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["hello, world", "left[right]", "left{right}", "a: b", "\ud83d\ude42, cat"]
    )
    fun composedYamlStringsStaySingleFlowValues(text: String) {
        writeComponent("source", "yaml", "value: \'" + text + "\'\n")
        writeComponent(
            "target",
            "yaml",
            "items: [\${source@value}\${missing@value:}]\nmapping: [{value: \${source@value}\${missing@value:}}]\n",
        )
        assertEquals(
            mapOf("items" to listOf(text), "mapping" to listOf(mapOf("value" to text))),
            build("target"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["hello, world", "left[right]", "left{right}", "\ud83d\ude42, cat"])
    fun propertiesTextStaysSingleFlowValue(text: String) {
        writeComponent("source", "properties", "value=" + text + "\n")
        writeComponent("target", "yaml", "items: [\${source@value}]\n")
        assertEquals(mapOf("items" to listOf(text)), build("target"))
    }

    @Test
    fun keepsNativeTypesAlongsideComputedFlowText() {
        writeComponent(
            "target",
            "yaml",
            "items: [#{2 + 3}, #{true}, #{\'hello, world\'}, [one, two]]\n",
        )
        assertEquals(
            listOf(5, true, "hello, world", listOf("one", "two")),
            build("target").get("items"),
        )
    }

    private fun writeComponent(component: String, extension: String, text: String) {
        val file: Path = root.resolve("components/" + component + "/application." + extension)
        Files.createDirectories(file.getParent())
        Files.writeString(file, text)
    }

    private fun build(component: String): Map<*, *> {
        val destination: Path = root.resolve("build")
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", listOf(), listOf())
        return Yaml().load(Files.readString(destination.resolve(component + "/application.yaml")))
    }
}
