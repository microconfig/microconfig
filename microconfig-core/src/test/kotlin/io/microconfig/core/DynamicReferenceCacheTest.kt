package io.microconfig.core

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.impl.MemoizingResolver
import io.microconfig.core.properties.resolvers.impl.PropertyResolutionCache
import io.microconfig.core.properties.resolvers.placeholder.impl.PlaceholderResolver
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.yaml.snakeyaml.Yaml

class DynamicReferenceCacheTest {
    @TempDir lateinit var root: Path

    @BeforeEach
    fun createEnvironment() {
        write("envs/dev.yaml", "group:\n  components: [app]\n")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun keepExplicitAndInheritedIncludeContextsForGeneratedPlaceholders(reverse: Boolean) {
        val references = listOf("implicit: \${source@value}\n", "explicit: \${source[dev]@value}\n")
        write(
            "components/app/application.yaml",
            (if (reverse) references.reversed() else references).joinToString(""),
        )
        write("components/source/application.yaml", "#include base[other]\n")
        write("components/base/application.other.yaml", "value: \"#{'$' + '{target@text}'}\"\n")
        write("components/target/application.other.yaml", "text: inherited\n")
        write("components/target/application.dev.yaml", "text: explicit\n")

        assertEquals(mapOf("implicit" to "inherited", "explicit" to "explicit"), build())
    }

    @ParameterizedTest
    @ValueSource(strings = ["this", "c"])
    fun resolveGeneratedSelfPlaceholdersUsingEachReferenceChain(component: String) {
        write("components/app/application.yaml", "p1: \${a@v}\np2: \${b@v}\n")
        write("components/a/application.yaml", "v: \${c@w}\nx: A\n")
        write("components/b/application.yaml", "v: \${c@w}\nx: B\n")
        write("components/c/application.yaml", "w: \"#{'$' + '{$component@x}'}\"\n")

        assertEquals(mapOf("p1" to "A", "p2" to "B"), build())
    }

    @Test
    fun resolveGeneratedCyclicDefaultsUsingEachReferenceChain() {
        write("components/app/application.yaml", "p1: \${a@x}\np2: \${a@y}\n")
        write(
            "components/a/application.yaml",
            "x: \"#{'$' + '{this@y:X}'}\"\ny: \"#{'$' + '{this@x:Y}'}\"\n",
        )

        assertEquals(mapOf("p1" to "Y", "p2" to "X"), build())
    }

    @Test
    fun avoidEvaluatingDefaultsInsideGeneratedPlaceholdersBeforeResolvingThem() {
        write(
            "components/app/application.yaml",
            "copy: \"#{'$' + '{target@text:#{1 / 0}}'}\"\n",
        )
        write("components/target/application.yaml", "text: present\n")

        assertEquals(mapOf("copy" to "present"), build())
    }

    @Test
    fun resolveExpressionDefaultsAfterGeneratedPlaceholderLookupFails() {
        write(
            "components/app/application.yaml",
            "copy: \"#{'$' + '{target@missing:#{1 + 2}}'}\"\n",
        )
        write("components/target/application.yaml", "text: present\n")

        assertEquals(mapOf("copy" to "3"), build())
    }

    @Test
    fun resolveExpressionsBeforeAndAfterGeneratedPlaceholders() {
        write(
            "components/app/application.yaml",
            "copy: \"#{1 + 2}-#{'$' + '{target@text}'}-#{3 + 4}\"\n",
        )
        write("components/target/application.yaml", "text: present\n")

        assertEquals(mapOf("copy" to "3-present-7"), build())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun preserveGeneratedCollectionDefaultTypes(quoted: Boolean) {
        val value = "#{'$' + '{target@missing:{port: 80}}'}"
        write(
            "components/app/application.yaml",
            "copy: " + (if (quoted) "\"$value\"" else value) + "\n",
        )
        write("components/target/application.yaml", "text: present\n")

        val expected = if (quoted) "{port: 80}" else mapOf("port" to 80)
        assertEquals(mapOf("copy" to expected), build())
    }

    @Test
    fun preserveGeneratedCollectionDefaultTypesInsideSequences() {
        write(
            "components/app/application.yaml",
            "items: [#{'$' + '{target@missing:{port: 80}}'}]\n",
        )
        write("components/target/application.yaml", "text: present\n")

        assertEquals(mapOf("items" to listOf(mapOf("port" to 80))), build())
    }

    @Test
    fun resolveGeneratedDefaultExpressionsBeforeLaterSequenceExpressions() {
        write(
            "components/app/application.yaml",
            "items: [#{'$' + '{target@missing:#{1 + 2}}'}, #{4 + 5}]\n",
        )
        write("components/target/application.yaml", "text: present\n")

        assertEquals(mapOf("items" to listOf(3, 9)), build())
    }

    @Test
    fun spliceListsAfterGeneratedPlaceholders() {
        write("components/source/application.yaml", "items: [one, two]\n")
        write("components/app/application.yaml", """
            items:
              #{'$' + '{source@items}'}
              - after
            """.trimIndent())

        assertEquals(mapOf("items" to listOf("one", "two", "after")), build())
    }

    @Test
    fun spliceGeneratedListsThroughReferencesAndTemplates() {
        write("components/base/application.yaml", "items: [one, two]\n")
        write("components/source/application.yaml", """
            items:
              #{'$' + '{base@items}'}
              - after
            """.trimIndent())
        write("components/app/application.yaml", """
            copy: ${'$'}{source@items}
            mc.template.list: ${'$'}{this@configDir}/list.txt -> rendered.yaml
            """.trimIndent())
        write("components/app/list.txt", "\${source@items}")

        assertEquals(mapOf("copy" to listOf("one", "two", "after")), build())
        assertEquals(listOf("one", "two", "after"),
            Yaml().load<List<String>>(Files.readString(root.resolve("output/app/rendered.yaml"))))
    }

    @Test
    fun evaluateExpressionOnlyPropertiesOnceAcrossDirectAndNestedReferences() {
        var evaluations = 0
        var generatedReferences = 0
        val expressions = object : RecursiveResolver {
            override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> {
                val match = Regex("#\\{(next|generated)}").find(line)
                return Optional.ofNullable(if (match == null) null else object : RecursiveResolver.Statement {
                    override val startIndex = match.range.first
                    override val endIndex = match.range.last + 1
                    override fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?) =
                        if (match.groupValues[1] == "next") (++evaluations).toString()
                        else "\${this@label}".also { generatedReferences++ }
                })
            }
        }
        fun property(component: String, key: String, value: String) =
            PropertyImpl.property(
                key, value, ConfigFormat.YAML, DeclaringComponentImpl("app", component, "dev"),
            )
        val stamp = property("app", "stamp", "#{next}")
        val independent = property("app", "other", "#{next}-#{next}")
        val collection = property("app", "collection", "[#{next}, #{next}]")
        val contextual = property("shared", "contextual", "#{next}-#{generated}-#{next}")
        val properties = mapOf(
            ("app" to "stamp") to stamp,
            ("app" to "other") to independent,
            ("app" to "collection") to collection,
            ("first" to "value") to property("first", "value", "\${app@stamp}"),
            ("second" to "value") to property("second", "value", "\${first@value}"),
            ("shared" to "contextual") to contextual,
            ("branchA" to "value") to property("branchA", "value", "\${shared@contextual}"),
            ("branchB" to "value") to property("branchB", "value", "\${shared@contextual}"),
            ("branchA" to "label") to property("branchA", "label", "A"),
            ("branchB" to "label") to property("branchB", "label", "B"),
        )
        val strategy = PlaceholderResolveStrategy { component, key, _, _, _ ->
            Optional.ofNullable(properties[component to key])
        }
        val cache = PropertyResolutionCache()
        val placeholders = PlaceholderResolver(
            mock(EnvironmentRepository::class.java), strategy, emptySet(), expressions, cache,
        )
        val resolver = MemoizingResolver(placeholders, cache, expressions)
        val context = DeclaringComponentImpl("app", "app", "dev")

        assertEquals("1", stamp.resolveBy(resolver, context).value)
        assertEquals("1", resolver.resolve("\${second@value}", context, context))
        assertEquals("1", resolver.resolve("\${first@value}", context, context))
        assertEquals("1", resolver.resolve("\${app@stamp}", context, context))
        assertEquals(1, evaluations)
        assertEquals("2-3", independent.resolveBy(resolver, context).value)
        assertEquals("2-3", resolver.resolve("\${app@other}", context, context))
        assertEquals(3, evaluations)
        assertEquals("4-A-5", resolver.resolve("\${branchA@value}", context, context))
        assertEquals("4-B-6", resolver.resolve("\${branchB@value}", context, context))
        assertEquals(6, evaluations)
        assertEquals(1, generatedReferences)
        assertEquals(listOf(7, 8), Yaml().load<List<Int>>(collection.resolveBy(resolver, context).value))
        assertEquals(listOf(7, 8), Yaml().load<List<Int>>(resolver.resolve("\${app@collection}", context, context)))
        assertEquals(8, evaluations)
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
