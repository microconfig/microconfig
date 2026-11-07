package io.microconfig

import io.microconfig.core.properties.io.selector.ConfigIoFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Random
import java.util.concurrent.TimeUnit.SECONDS

class ParallelExpressionCacheTest {
    @TempDir
    lateinit var directory: Path

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 4, 8, 32])
    fun finishParallelStreamsWithGeneratedReferencesToCachedExpressions(parallelism: Int) {
        val root = createRepository()

        assertBuildCompletes(root, parallelism, "yaml", names, expressionCount * 2, "3")
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 2, 4, 8, 32])
    fun finishParallelStreamsWhenExpressionInterpolationProducesWholePlaceholders(parallelism: Int) {
        val root = createBoundaryChangingRepository(names)

        assertBuildCompletes(root, parallelism, "properties", names, expressionCount * 3, "2")
    }

    @Test
    fun finishSequentialBuildWhenExpressionInterpolationProducesWholePlaceholders() {
        val selected = listOf("service0")
        val root = createBoundaryChangingRepository(selected)

        assertBuildCompletes(root, 1, "properties", selected, expressionCount * 3, "2")
    }

    private fun createRepository(): Path {
        val root = createEnvironment(names)
        val random = Random(1)
        val properties = (0 until expressionCount).joinToString("") { index ->
            val expression = "#{#{'$' + '{this@operand$index}'} + 1}"
            val value = if (index % 2 == 0) expression else "\${this@missingResult$index:$expression}"
            "value$index: $value-\${this@name}\n" +
                "operand$index: #{1 + 1}\${this@absent${random.nextInt(1_000_000)}:}\n"
        }
        write(root.resolve("components/original/application.yaml"), properties)
        return root
    }

    private fun createEnvironment(components: List<String>): Path {
        val root = directory.resolve("repo")
        write(root.resolve("envs/dev.yaml"),
            "group:\n  components: [${components.joinToString(", ") { "$it:original" }}]\n")
        return root
    }

    private fun write(file: Path, text: String) {
        Files.createDirectories(file.parent)
        Files.writeString(file, text)
    }

    private fun createBoundaryChangingRepository(components: List<String>): Path {
        val root = createEnvironment(components)
        val random = Random(1)
        val properties = (0 until expressionCount).joinToString("") { index ->
            val expression = "#{\${this@code$index}}"
            val value = if (index % 2 == 0) expression else "\${this@missingResult$index:$expression}"
            "value$index=$value-\${this@name}\n" +
                "code$index='$' + '{this@operand$index'}\n" +
                "operand$index=#{1 + 1}\${this@absent${random.nextInt(1_000_000)}:}\n"
        }
        write(root.resolve("components/original/application.properties"), properties)
        return root
    }

    private fun assertBuildCompletes(
        root: Path,
        parallelism: Int,
        extension: String,
        components: List<String>,
        propertyCount: Int,
        expectedValue: String,
    ) {
        val destination = directory.resolve("output")
        val log = directory.resolve("cli.log")
        val process = startCli(root, destination, log, parallelism)
        try {
            assertTrue(process.waitFor(15, SECONDS), "CLI deadlocked with parallelism=$parallelism; ${Files.readString(log)}")
            assertEquals(0, process.exitValue(), Files.readString(log))
            assertGeneratedValues(destination, extension, components, propertyCount, expectedValue)
        } finally {
            process.destroyForcibly()
            assertTrue(process.waitFor(5, SECONDS), "CLI process did not stop")
        }
    }

    private fun startCli(root: Path, destination: Path, log: Path, parallelism: Int): Process =
        ProcessBuilder(
            File(System.getProperty("java.home"), "bin/java").path,
            "-Djava.util.concurrent.ForkJoinPool.common.parallelism=$parallelism",
            "-jar", requireNotNull(System.getProperty("microconfig.cli.jar")),
            "-r", root.toString(), "-d", destination.toString(), "-e", "dev", "-color", "false",
        ).redirectErrorStream(true).redirectOutput(log.toFile()).start()

    private fun assertGeneratedValues(
        destination: Path,
        extension: String,
        components: List<String>,
        propertyCount: Int,
        expectedValue: String,
    ) {
        val configIo = ConfigIoFactory.configIo()
        components.forEach { name ->
            val output = destination.resolve("$name/application.$extension")
            val values = configIo.readFrom(output.toFile()).propertiesAsMap()
            assertEquals(propertyCount, values.size, name)
            (0 until expressionCount).forEach { index ->
                assertEquals("$expectedValue-$name", values["value$index"], "$name/value$index")
                assertEquals("2", values["operand$index"], "$name/operand$index")
            }
        }
    }

    companion object {
        private const val expressionCount = 16
        private val names = (0..31).map { "service$it" }
    }
}
