package io.microconfig.core

import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ResolveException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BuildLifecycleTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createRepository() {
        Files.createDirectories(root.resolve("envs"))
        Files.createDirectories(root.resolve("components/app"))
        Files.writeString(root.resolve("envs/dev.yaml"), "group:\n  components: [app]\n")
        Files.writeString(root.resolve("envs/prod.yaml"), "group:\n  components: [app]\n")
        source("value: first\n")
    }

    @Test
    fun rejectSecondBuildBeforeOverwritingFiles() {
        val runner = MicroconfigRunner(microconfig())
        runner.build("dev", emptyList(), emptyList())
        source("value: changed\n")

        val error = assertThrows(IllegalStateException::class.java) {
            runner.build("dev", emptyList(), emptyList())
        }

        assertTrue(error.message.orEmpty().contains("new Microconfig"), error.message)
        assertEquals("value: first\n", Files.readString(root.resolve("output/app/application.yaml")))
    }

    @Test
    fun shareTheGuardBetweenRunnersAndBuildEntryPoints() {
        val microconfig = microconfig()
        MicroconfigRunner(microconfig).buildProperties("dev", emptyList(), emptyList())

        assertThrows(IllegalStateException::class.java) {
            MicroconfigRunner(microconfig).build("prod", emptyList(), emptyList())
        }
        assertFalse(Files.exists(root.resolve("output/app/application.yaml")))
    }

    @Test
    fun consumeTheInstanceAfterFailureAndAllowANewInstanceToReadFixedSources() {
        val runner = MicroconfigRunner(microconfig())
        source("value: #{1 / 0}\n")
        assertThrows(ResolveException::class.java) {
            runner.buildProperties("dev", emptyList(), emptyList())
        }
        source("value: fixed\n")

        assertThrows(IllegalStateException::class.java) {
            runner.buildProperties("dev", emptyList(), emptyList())
        }
        val result = MicroconfigRunner(microconfig()).buildProperties("dev", emptyList(), emptyList())
        assertEquals("fixed", result.propertiesAsKeyValue["value"])
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun freshBuildReadsChangedProfilesAndNewComponentFiles(copyInstance: Boolean) {
        Files.writeString(root.resolve("envs/dev.yaml"), "profiles: [base]\ngroup:\n  components: [app]\n")
        Files.writeString(root.resolve("envs/base.yaml"), "abstract: true\n")
        Files.writeString(root.resolve("components/app/application.base.yaml"), "fromBase: original\n")
        val initial = microconfig()
        val first = MicroconfigRunner(initial).buildProperties("dev", emptyList(), emptyList())
        assertEquals(listOf("base"), initial.environments().profilesOf("dev"))
        assertEquals(mapOf("value" to "first", "fromBase" to "original"), first.propertiesAsKeyValue)

        Files.writeString(root.resolve("envs/base.yaml"), "abstract: true\nprofiles: [common]\n")
        Files.writeString(root.resolve("envs/common.yaml"), "abstract: true\n")
        Files.writeString(root.resolve("components/app/application.common.yaml"), "fromCommon: added\n")
        Files.writeString(root.resolve("components/app/config.process"), "heap=512m\n")
        val next = if (copyInstance) initial.withDestinationDir(root.resolve("next-output").toFile())
            else microconfig()

        val second = MicroconfigRunner(next).buildProperties("dev", emptyList(), emptyList())

        assertEquals(listOf("common", "base"), next.environments().profilesOf("dev"))
        assertEquals(
            mapOf("value" to "first", "fromBase" to "original", "fromCommon" to "added", "heap" to "512m"),
            second.propertiesAsKeyValue,
        )
        assertEquals(listOf("base"), initial.environments().profilesOf("dev"))
    }

    @Test
    fun rejectConcurrentBuildsUsingTheSameInstance() {
        source("value: \${test@value}\n")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val strategy = PlaceholderResolveStrategy { component, _, _, _, _ ->
            if (component != "test") Optional.empty()
            else {
                entered.countDown()
                check(release.await(5, SECONDS))
                Optional.of(PropertyImpl.property("value", "resolved", ConfigFormat.YAML, null))
            }
        }
        val microconfig = microconfig().withAdditionalPlaceholderResolvers(listOf(strategy))
        val executor = Executors.newSingleThreadExecutor()
        try {
            val first = executor.submit<io.microconfig.core.properties.Properties> {
                MicroconfigRunner(microconfig).buildProperties("dev", emptyList(), emptyList())
            }
            assertTrue(entered.await(5, SECONDS))
            assertThrows(IllegalStateException::class.java) {
                MicroconfigRunner(microconfig).buildProperties("dev", emptyList(), emptyList())
            }
            release.countDown()
            assertEquals("resolved", first.get(5, SECONDS).propertiesAsKeyValue["value"])
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private fun source(content: String) = Files.writeString(root.resolve("components/app/application.yaml"), content)

    private fun microconfig() = Microconfig.searchConfigsIn(root.toFile())
        .withDestinationDir(root.resolve("output").toFile())
}
