package io.microconfig.core

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.Microconfig.Companion.searchConfigsIn
import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.serializers.PropertySerializers.asString
import io.microconfig.utils.FileUtils.walk
import io.microconfig.utils.IoUtils.readFully
import java.io.File
import java.lang.Math.min
import java.nio.file.Path
import java.util.stream.Collectors.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class MicroconfigTest {
    val root: File = classpathFile("repo")

    val microconfig: Microconfig = searchConfigsIn(root)

    @TestFactory
    fun findTests(): List<DynamicTest> {
        walk(root.toPath()).use { stream ->
            return stream
                .map(Path::toFile)
                .filter(this::isExpectation)
                .map(this::toTest)
                .collect(toList())
        }
    }

    @Test
    fun testIncorrectRootDir() {
        assertThrows(IllegalArgumentException::class.java, { searchConfigsIn(File("missingDir")) })
    }

    private fun isExpectation(file: File): Boolean {
        val name: String = file.name
        return overriddenTest(file) &&
            (name.startsWith("expect.") ||
                name.startsWith("truncate.") ||
                name.startsWith("exception."))
    }

    private fun overriddenTest(file: File): Boolean {
        val testName: String? = System.getProperty("test")
        return testName == null || file.parentFile.absolutePath.contains(testName)
    }

    private fun toTest(expectation: File): DynamicTest {
        val component: String = getComponentName(expectation)
        val env: String = getEnvName(expectation)
        return toTest(expectation, component, env)
    }

    private fun toTest(expectation: File, component: String, env: String): DynamicTest {
        return dynamicTest(
            component + "[" + env + "]",
            {
                val expected: String = readExpectation(expectation).replace("\r\n", "\n").trim()
                var actual: String = build(component, env).replace("\r\n", "\n").trim()
                if ((expectation.name.startsWith("truncate."))) {
                    actual = actual.substring(0, min(expected.length, actual.length))
                }
                assertEquals(expected, actual)
            },
        )
    }

    private fun getComponentName(expectation: File): String {
        val parts = expectation.name.split('.')
        return if (parts.size == 3) parts[2] else expectation.parentFile.name
    }

    private fun getEnvName(expectation: File): String {
        return expectation.name.split('.')[1]
    }

    private fun build(component: String, env: String): String {
        try {
            return microconfig
                .environments()
                .getOrCreateByName(env)
                .findComponentWithName(component)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .first()
                .save(asString())
        } catch (e: RuntimeException) {
            return e.message.orEmpty()
        }
    }

    private fun readExpectation(expectation: File): String {
        return readFully(expectation)
            .replace("\${configDir}", toPath(expectation.parentFile))
            .replace("\${configRoot}", toPath(root))
            .replace("\${resultDir}", toPath(File(root, "build")))
            .replace("\${userHome}", toPath(File(System.getProperty("user.home"))))
            .replace("\${space}", " ")
            .replace("#todo", "")
    }

    private fun toPath(file: File): String {
        return file.absolutePath.replace('\\', '/')
    }
}
