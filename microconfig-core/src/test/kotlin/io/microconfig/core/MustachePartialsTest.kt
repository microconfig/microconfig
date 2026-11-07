package io.microconfig.core

import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.properties.templates.TemplateDefinition
import io.microconfig.core.properties.templates.TemplatePattern
import io.microconfig.core.properties.templates.impl.MustacheTemplateProcessor
import io.microconfig.core.properties.templates.impl.StringTemplate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Files
import java.nio.file.Path

class MustachePartialsTest {
    @TempDir
    lateinit var directory: Path
    private val root
        get() = directory.resolve("repo")

    private val destination
        get() = directory.resolve("output")

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @CsvSource("mc.mustache,txt", "mc.template,mustache")
    fun resolveMicroconfigPlaceholdersInNestedPartials(prefix: String, extension: String) {
        write(
            "components/app/application.yaml",
            "value: world\n$prefix.t: \${this@configDir}/main.$extension -> rendered.txt\n",
        )
        write("components/app/main.$extension", "hello {{>partial.txt}}")
        write(
            "components/app/partial.txt",
            "\${this@value}|\${value}|\${this@name}|\${this@templateName}|" +
                    "\${this@missing:fallback}|\\\${this@value}|{{value}}|{{>nested.txt}}|\${UNKNOWN}",
        )
        write("components/app/nested.txt", "\${this@value}")

        build()

        assertEquals(
            "hello world|world|app|t|fallback|\${this@value}|world|world|\${UNKNOWN}",
            Files.readString(destination.resolve("app/rendered.txt")),
        )
    }

    @Test
    fun reportMissingRequiredPlaceholderInPartialBeforeWriting() {
        write(
            "components/app/application.yaml",
            "value: world\nmc.mustache.t: \${this@configDir}/main.txt -> rendered.txt\n",
        )
        write("components/app/main.txt", "{{>partial.txt}}")
        write("components/app/partial.txt", "\${this@missing}")

        assertThrows(IllegalStateException::class.java) { build() }

        assertFalse(Files.exists(destination.resolve("app/rendered.txt")))
        assertFalse(Files.exists(destination.resolve("app/application.yaml")))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun configurePartialsEquallyForDefaultAndExplicitProcessor(explicit: Boolean) {
        write("components/app/application.yaml", "value: world\n")
        write("components/app/main.txt", "hello {{>partial.txt}}")
        write("components/app/partial.txt", "\${this@value}|\${this@templateName}")
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        val properties = microconfig.inEnvironment("dev").getComponentWithName("app")
            .getPropertiesFor(configType(APPLICATION)).resolveBy(microconfig.resolver()).first()
        val definition =
            if (explicit) TemplateDefinition(
                "mustache",
                "t",
                TemplatePattern.defaultPattern(),
                MustacheTemplateProcessor()
            )
            else TemplateDefinition("mustache", "t", TemplatePattern.defaultPattern())
        definition.setFromFile(root.resolve("components/app/main.txt").toString())
        definition.setToFile("rendered.txt")

        assertEquals("hello world|t", definition.resolve(microconfig.resolver(), properties).content)
    }

    @Test
    fun letCustomPartialProcessorResolvePlaceholdersExactlyOnce() {
        write("components/app/application.yaml", "value: world\n")
        write("components/app/main.txt", "hello {{>partial.txt}}|{{>second.txt}}")
        write(
            "components/app/partial.txt",
            "\\\${this@value}|\${this@value}|\${this@templateName}|{{value}}|{{>nested.txt}}"
        )
        write("components/app/nested.txt", "\\\${this@value}|\${this@value}")
        write("components/app/second.txt", "\${this@value}")
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        val properties = microconfig.inEnvironment("dev").getComponentWithName("app")
            .getPropertiesFor(configType(APPLICATION)).resolveBy(microconfig.resolver()).first()
        val pattern = TemplatePattern.defaultPattern()
        val source = root.resolve("components/app/main.txt").toFile()
        val target = destination.resolve("rendered.txt").toFile()
        var calls = 0
        val processor = MustacheTemplateProcessor { content ->
            calls++
            StringTemplate("t", source, target, pattern.placeholderPattern, content)
                .resolveBy(microconfig.resolver(), properties.declaringComponent).content
        }
        val definition = TemplateDefinition("mustache", "t", pattern, processor)
        definition.setFromFile(source.path)
        definition.setToFile(target.path)

        val rendered = definition.resolve(microconfig.resolver(), properties).content

        assertEquals("hello \${this@value}|world|t|world|\${this@value}|world|world", rendered)
        assertEquals(3, calls)
    }

    @Test
    fun preserveCustomPartialProcessingWithoutAddingAnImplicitSecondPass() {
        write("components/app/application.yaml", "value: world\n")
        write("components/app/main.txt", "hello {{>partial.txt}}")
        write("components/app/partial.txt", "token \${this@value}|{{value}}")
        val microconfig = Microconfig.searchConfigsIn(root.toFile())
        val properties = microconfig.inEnvironment("dev").getComponentWithName("app")
            .getPropertiesFor(configType(APPLICATION)).resolveBy(microconfig.resolver()).first()
        val processor = MustacheTemplateProcessor { it.replace("token", "custom") }
        val definition = TemplateDefinition("mustache", "t", TemplatePattern.defaultPattern(), processor)
        definition.setFromFile(root.resolve("components/app/main.txt").toString())
        definition.setToFile(destination.resolve("rendered.txt").toString())

        assertEquals(
            "hello custom \${this@value}|world",
            definition.resolve(microconfig.resolver(), properties).content
        )
    }

    private fun build() =
        MicroconfigRunner(root.toFile(), destination.toFile()).build("dev", emptyList(), emptyList())

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }
}
