package io.microconfig

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit

@EnabledOnOs(OS.LINUX, OS.MAC)
class PublishingPropertiesScriptTest {
    @TempDir
    lateinit var directory: Path
    private val propertiesFile
        get() = directory.resolve("gradle.properties")

    @BeforeEach
    fun createPropertiesFile() {
        Files.writeString(propertiesFile, "# publication\nossrhPassword=\nversion=5.0.0\n")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "abc&def", "abc+def", "abc/def=", "C:\\temp\\new", "'\"\$`value`",
            " leading ", "\tleading", "first\nsecond\rthird\tend\u000C", "",
            "пароль", "密码", "🚀", "mixed-€-😀-\\u0041",
        ]
    )
    fun preserveLiteralPropertyValues(value: String) {
        setProperty("ossrhPassword", value)

        val properties = readProperties()
        assertEquals(value, properties.getProperty("ossrhPassword"))
        assertEquals("5.0.0", properties.getProperty("version"))
        assertTrue(Files.readString(propertiesFile).startsWith("# publication\n"))
    }

    @Test
    fun replaceExistingValueAndMatchPropertyNameLiterally() {
        Files.writeString(propertiesFile, "signing.password=old\nsigningXpassword=untouched\n")

        setProperty("signing.password", "first&value")
        setProperty("signing.password", "second+value")

        assertEquals("second+value", readProperties().getProperty("signing.password"))
        assertEquals("untouched", readProperties().getProperty("signingXpassword"))
    }

    @Test
    fun addMissingProperty() {
        setProperty("signing.password", "new+value")

        assertEquals("new+value", readProperties().getProperty("signing.password"))
    }

    private fun setProperty(key: String, value: String) {
        val script = Path.of("../.github/scripts/set_gradle_properties.sh").toAbsolutePath()
        val process = ProcessBuilder("sh", script.toString(), key, value)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .start()
        assertTrue(process.waitFor(10, TimeUnit.SECONDS), "Property writer timed out")
        assertEquals(0, process.exitValue(), process.inputStream.bufferedReader().use { it.readText() })
    }

    private fun readProperties(): Properties = Properties().apply {
        Files.newInputStream(propertiesFile).use { load(it) }
    }
}
