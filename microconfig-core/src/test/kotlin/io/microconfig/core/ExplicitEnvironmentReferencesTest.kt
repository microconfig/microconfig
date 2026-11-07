package io.microconfig.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

class ExplicitEnvironmentReferencesTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createEnvironments() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("envs/prod.yaml", "{}\n")
        write("envs/test.yaml", "{}\n")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepExplicitEnvironmentThroughoutReferences(withProfile: Boolean) {
        if (withProfile)
            write("envs/dev.yaml", "profiles: [prod]\ngroup:\n  components: [app]\n")
        write(
            "components/app/application.yaml",
            "self: \${this[prod]@value}\nother: \${source[prod]@value}\n" +
                    "implicit: \${source@value}\ncurrent: \${this@value}\n" +
                    "environment: \${this[prod]@env}\n",
        )
        write("components/app/application.dev.yaml", "value: app-dev\n")
        write("components/app/application.prod.yaml", "value: app-prod\n")
        write("components/source/application.yaml", "value: \${other@text}\n")
        write("components/other/application.dev.yaml", "text: other-dev\n")
        write("components/other/application.prod.yaml", "text: other-prod\n")

        assertEquals(
            mapOf(
                "self" to "app-prod", "other" to "other-prod", "implicit" to "other-dev",
                "current" to "app-dev", "environment" to "prod", "value" to "app-dev",
            ),
            build(),
        )
    }

    @Test
    fun avoidFallingBackToCurrentEnvironmentInsideExplicitReference() {
        write("components/app/application.yaml", "ref: \${source[prod]@value}\n")
        write("components/app/application.dev.yaml", "setting: app-dev\n")
        write("components/source/application.yaml", "value: \${this@setting}\n")
        write("components/source/application.prod.yaml", "setting: source-prod\n")

        assertEquals(mapOf("ref" to "source-prod", "setting" to "app-dev"), build())
    }

    @Test
    fun allowNestedExplicitEnvironmentAndRestoreOuterContext() {
        write(
            "components/app/application.yaml",
            "explicit: \${source[prod]@value}\nimplicit: \${source@value}\n",
        )
        write("components/source/application.yaml", "value: \${other@text}\n")
        write("components/source/application.prod.yaml", "value: \${other[test]@text}\n")
        write("components/other/application.dev.yaml", "text: dev\n")
        write("components/other/application.test.yaml", "text: test\n")

        assertEquals(mapOf("explicit" to "test", "implicit" to "dev"), build())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepExplicitAndInheritedIncludeContextsSeparateInCache(reverse: Boolean) {
        val references = listOf("implicit: \${source@value}\n", "explicit: \${source[dev]@value}\n")
        write(
            "components/app/application.yaml",
            (if (reverse) references.reversed() else references).joinToString(""),
        )
        write("components/source/application.yaml", "#include base[other]\n")
        write("components/base/application.other.yaml", "value: \${target@text}\n")
        write("components/target/application.other.yaml", "text: inherited\n")
        write("components/target/application.dev.yaml", "text: explicit\n")

        assertEquals(mapOf("implicit" to "inherited", "explicit" to "explicit"), build())
    }

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build(): Map<*, *> {
        val destination = root.resolve("output")
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", emptyList(), emptyList())
        return Yaml().load(Files.readString(destination.resolve("app/application.yaml")))
    }
}
