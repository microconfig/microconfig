package io.microconfig

import io.microconfig.utils.IoUtils.readClasspathResource
import io.microconfig.utils.IoUtils.readFully
import java.io.File
import java.io.StringReader
import java.util.Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MicroconfigMainTest {
    @TempDir lateinit var destinationDir: File

    lateinit var root: String

    @BeforeEach
    fun setup() {
        root = javaClass.classLoader.getResource("repo").file
    }

    @Test
    fun should_package_only_project_version_in_version_resource() {
        val properties: Properties = Properties()
        properties.load(StringReader(readClasspathResource("version.properties")))
        assertEquals(setOf("version"), properties.stringPropertyNames())
        assertFalse(properties.getProperty("version").isBlank())
    }

    @Test
    fun should_generate_config_for_single_env_when_e_param_given() {
        MicroconfigMain.main(
            "-e",
            "env1",
            "-r",
            escape(root),
            "-d",
            escape(destinationDir.absolutePath),
        )
        checkConfigGeneratedFor("component", null)
        checkConfigGeneratedFor("component2", null)
    }

    @Test
    fun should_generate_config_for_multiple_envs_when_envs_param_given() {
        val environments: List<String> = listOf("env2", "env3")
        MicroconfigMain.main(
            "-envs",
            environments.joinToString(","),
            "-r",
            escape(root),
            "-d",
            escape(destinationDir.absolutePath),
        )
        for (component in components) {
            for (environment in environments) {
                checkConfigGeneratedFor(component, environment)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["env1, *", "*, env1", "*"])
    fun should_generate_config_for_all_envs_when_star_in_envs_list(envs: String) {
        MicroconfigMain.main(
            "-envs",
            envs,
            "-r",
            escape(root),
            "-d",
            escape(destinationDir.absolutePath),
        )
        for (component in components) {
            for (environment in listOf("env1", "env2")) {
                checkConfigGeneratedFor(component, environment)
            }
        }
    }

    @Test
    fun should_not_generate_config_for_excluded_envs() {
        val envs: String = "env1, !env2, env3"
        MicroconfigMain.main(
            "-envs",
            envs,
            "-r",
            escape(root),
            "-d",
            escape(destinationDir.absolutePath),
        )
        for (component in components) {
            for (environment in listOf("env1", "env3")) {
                checkConfigGeneratedFor(component, environment)
            }
            checkConfigNotGeneratedFor(component, "env2")
        }
    }

    @Test
    fun excludeBrokenDescriptorsBeforeResolvingEnvironmentIncludes() {
        val repository = File(destinationDir, "input")
        val output = File(destinationDir, "output")
        File(repository, "envs").mkdirs()
        File(repository, "components/app").mkdirs()
        File(repository, "envs/dev.yaml").writeText("group:\n  components: [app]\n")
        File(repository, "envs/broken.yaml").writeText("include:\n  env: missing\n")
        File(repository, "components/app/application.yaml").writeText("key: value\n")
        MicroconfigMain.main("-envs", "*,!broken", "-r", repository.path, "-d", output.path)
        assertEquals("key: value\n", File(output, "dev/app/application.yaml").readText())
    }

    private fun escape(name: String): String {
        return "\"" + name + "\""
    }

    private fun checkConfigGeneratedFor(component: String, nestedDirectory: String?) {
        checkComponentBuildResult(component, nestedDirectory, true)
    }

    private fun checkConfigNotGeneratedFor(component: String, nestedDirectory: String?) {
        checkComponentBuildResult(component, nestedDirectory, false)
    }

    private fun checkComponentBuildResult(
        component: String,
        nestedDirectory: String?,
        shouldFileExist: Boolean,
    ) {
        checkFileExist(
            buildFilePath(component, "application.yaml", nestedDirectory),
            shouldFileExist,
        )
        checkFileExist(buildFilePath(component, "deploy.yaml", nestedDirectory), shouldFileExist)
    }

    private fun buildFilePath(
        component: String,
        fileName: String,
        nestedDirectory: String?,
    ): String {
        var result: String = String.format("%s/%s/", component, fileName)
        if ((nestedDirectory != null)) {
            result = String.format("%s/%s", nestedDirectory, result)
        }
        return result
    }

    private fun checkFileExist(filePath: String, shouldFileExist: Boolean) {
        val resultFile: File = File(destinationDir, filePath)
        assertEquals(shouldFileExist, resultFile.exists())
        if ((shouldFileExist)) {
            assertEquals("key: " + resultFile.nameWithoutExtension, readFully(resultFile).trim())
        }
    }

    companion object {
        val components: List<String> = listOf("component", "component2")
    }
}
