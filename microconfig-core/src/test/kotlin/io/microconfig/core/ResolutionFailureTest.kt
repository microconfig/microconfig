package io.microconfig.core

import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.ResolveException
import io.microconfig.core.properties.templates.TemplatePattern
import io.microconfig.core.properties.templates.impl.StringTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path

class ResolutionFailureTest {
    @TempDir
    lateinit var root: Path

    @BeforeEach
    fun createRepository() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
        write("components/app/application.yaml", "value: \${source@value:fallback}\n")
    }

    @ParameterizedTest
    @ValueSource(strings = ["value: #{1 / 0}\n", "value: [unterminated\n", "#include missing\nvalue: present\n"])
    fun reportBrokenReferencedConfigurationInsteadOfUsingDefault(content: String) {
        write("components/source/application.yaml", content)
        assertThrows(ResolveException::class.java) { build() }
    }

    @Test
    fun useDefaultWhenReferencedComponentOrPropertyIsMissing() {
        assertEquals("fallback", build().propertiesAsKeyValue["value"])
        write("components/source/application.yaml", "other: present\n")
        assertEquals("fallback", build().propertiesAsKeyValue["value"])
    }

    @Test
    fun useSelfDefaultForMissingValuesButPropagateErrorsFromExistingValues() {
        write("components/app/application.yaml", "value: \${this@missing:fallback}\n")
        assertEquals("fallback", build().propertiesAsKeyValue["value"])

        write("components/app/application.yaml", "value: \${this@broken:fallback}\nbroken: #{1 / 0}\n")
        assertThrows(ResolveException::class.java) { build() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["source@value", "this@value"])
    fun reportIoFailureForReferencedAndOverridableValues(reference: String) {
        write("components/app/application.yaml", "value: \${$reference:fallback}\n")
        val microconfig = withFailingStrategy()
        assertThrows(ResolveException::class.java) {
            MicroconfigRunner(microconfig).buildProperties("dev", emptyList(), emptyList())
        }
    }

    @Test
    fun reportIoFailureInShortTemplatePlaceholders() {
        val template = StringTemplate(
            "test", File("source"), File("destination"),
            TemplatePattern.defaultPattern().placeholderPattern, "\${value}"
        )
        val context = DeclaringComponentImpl("app", "app", "dev")
        assertThrows(ResolveException::class.java) {
            template.resolveBy(withFailingStrategy().resolver(), context)
        }
    }

    private fun withFailingStrategy() = Microconfig.searchConfigsIn(root.toFile())
        .withAdditionalPlaceholderResolvers(listOf(PlaceholderResolveStrategy { _, _, _, _, _ ->
            throw UncheckedIOException(IOException("Source is unavailable"))
        }))

    private fun write(path: String, content: String) {
        val file = root.resolve(path)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    private fun build() = MicroconfigRunner(root.toFile(), root.resolve("out").toFile())
        .buildProperties("dev", emptyList(), emptyList())
}
