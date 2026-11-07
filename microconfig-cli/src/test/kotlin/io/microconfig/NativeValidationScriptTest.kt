package io.microconfig

import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir

@EnabledOnOs(OS.LINUX, OS.MAC)
class NativeValidationScriptTest {
    @TempDir lateinit var directory: Path

    @Test
    fun executeAllNegativeExpressionScenariosAndRejectUnrelatedFailures() {
        val script = Path.of("src/test/python/test_verify_native.py").toAbsolutePath()
        val output = directory.resolve("python-tests.log").toFile()
        val process = ProcessBuilder("python3", script.toString(), "-v")
            .redirectErrorStream(true).redirectOutput(output)
            .apply {
                environment()["MICROCONFIG_CLI_JAR"] = requireNotNull(System.getProperty("microconfig.cli.jar"))
                environment()["MICROCONFIG_JAVA"] = Path.of(System.getProperty("java.home"), "bin", "java").toString()
                environment()["PYTHONDONTWRITEBYTECODE"] = "1"
            }.start()

        assertTrue(process.waitFor(60, SECONDS), "Native validation regression tests timed out")
        assertEquals(0, process.exitValue(), output.readText())
    }
}
