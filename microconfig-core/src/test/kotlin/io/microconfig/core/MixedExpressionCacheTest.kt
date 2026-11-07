package io.microconfig.core

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.*
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.impl.MemoizingResolver
import io.microconfig.core.properties.resolvers.impl.PropertyResolutionCache
import io.microconfig.core.properties.resolvers.placeholder.impl.PlaceholderResolver
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.yaml.snakeyaml.Yaml

class MixedExpressionCacheTest {
    private val context = DeclaringComponentImpl("app", "app", "dev")
    private val evaluations = AtomicInteger()
    private val properties = mutableMapOf<Pair<String, String>, Property>()
    private val expressions = object : RecursiveResolver {
        private val delegate = ExpressionResolver()

        override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
            delegate.findStatementIn(line).map { statement ->
                object : RecursiveResolver.Statement {
                    override val startIndex = statement.startIndex
                    override val endIndex = statement.endIndex
                    override fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?): String {
                        val expression = line.subSequence(startIndex, endIndex).toString()
                        return when {
                            expression == "#{next}" -> evaluations.incrementAndGet().toString()
                            expression.startsWith("#{next + ") -> delegate.resolve(
                                expression.replaceFirst("next", evaluations.incrementAndGet().toString()), sourceOfValue, root)
                            else -> statement.resolveFor(sourceOfValue, root)
                        }
                    }
                }
            }
    }
    private val cache = PropertyResolutionCache()
    private val placeholders = PlaceholderResolver(mock(EnvironmentRepository::class.java),
        PlaceholderResolveStrategy { component, key, _, _, _ -> Optional.ofNullable(properties[component to key]) },
        emptySet(), expressions, cache)
    private val resolver = MemoizingResolver(placeholders, cache, expressions)

    @ParameterizedTest
    @EnumSource(ValueFormat::class)
    fun reuseIndependentExpressionAcrossDirectAndNestedReferences(format: ValueFormat) {
        val stamp = property("app", "stamp", "#{next}-\${this@label}", format)
        property("app", "label", "app")
        property("first", "value", "\${app@stamp}")
        property("second", "value", "\${first@value}")

        val direct = stamp.resolveBy(resolver, context).value

        assertEquals("1-app", decoded(direct, format))
        assertEquals("1-app", reference("second", "value"))
        assertEquals("1-app", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @ParameterizedTest
    @EnumSource(ValueFormat::class)
    fun reuseDefaultExpressionsAcrossDirectAndNestedReferences(format: ValueFormat) {
        val stamp = property("app", "stamp", "\${this@missing:#{next}}-stamp", format)
        property("first", "value", "\${app@stamp}")
        property("second", "value", "\${first@value}")

        assertEquals("1-stamp", decoded(stamp.resolveBy(resolver, context).value, format))
        assertEquals("1-stamp", reference("second", "value"))
        assertEquals("1-stamp", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun reuseExpressionsInsideNestedDefaults() {
        val stamp = property("app", "stamp", "\${this@missing:\${this@alsoMissing:#{next}}}")

        assertEquals("1", stamp.resolveBy(resolver, context).value)
        assertEquals("1", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "\${this@missing:[#{next}, #{next}]}",
        "[\${this@missing:#{next}}, \${this@missing:#{next}}]",
        "\${this@missing:[\${this@nested:#{next}}, #{next}]}",
    ])
    fun keepDefaultExpressionOccurrencesDistinctInsideCollections(value: String) {
        val stamp = property("app", "stamp", value, ValueFormat.YAML)
        val expected = listOf(1, 2)

        assertEquals(expected, Yaml().load<List<Any>>(stamp.resolveBy(resolver, context).value))
        assertEquals(expected, Yaml().load<List<Any>>(reference("app", "stamp")))
        assertEquals(2, evaluations.get())
    }

    @Test
    fun reuseExpressionsInsideAMappingDefault() {
        val stamp = property("app", "stamp", "\${this@missing:{first: #{next}, second: #{next}}}", ValueFormat.YAML)
        val expected = mapOf("first" to 1, "second" to 2)

        assertEquals(expected, Yaml().load<Map<String, Any>>(stamp.resolveBy(resolver, context).value))
        assertEquals(expected, Yaml().load<Map<String, Any>>(reference("app", "stamp")))
        assertEquals(2, evaluations.get())
    }

    @Test
    fun keepDefaultsLazyAfterAnEarlierReferenceDidNotUseThem() {
        property("shared", "stamp", "\${this@label:#{next}}")
        property("first", "value", "\${shared@stamp}")
        property("first", "label", "present")
        property("second", "value", "\${shared@stamp}")
        property("third", "value", "\${shared@stamp}")

        assertEquals("present", reference("first", "value"))
        assertEquals(0, evaluations.get())
        assertEquals("1", reference("second", "value"))
        assertEquals("1", reference("third", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun preserveContextualPlaceholdersInsideCachedDefaults() {
        property("shared", "stamp", "\${this@missing:#{next}-\${this@label}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("1-B", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun preserveContextDependentExpressionsInsideDefaults() {
        property("shared", "stamp", "\${this@missing:#{'\${this@label}'.toUpperCase()}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "a")
        property("second", "label", "b")

        assertEquals("A", reference("first", "value"))
        assertEquals("B", reference("second", "value"))
    }

    @Test
    fun cacheDefaultExpressionsByTheirResolvedArguments() {
        property("shared", "stamp", "\${this@missing:#{next + '-' + '\${this@label}'}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("third", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")
        property("third", "label", "A")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("2-B", reference("second", "value"))
        assertEquals("1-A", reference("third", "value"))
        assertEquals(2, evaluations.get())
    }

    @Test
    fun cacheNestedDefaultExpressionsWithoutMixingContextualArguments() {
        property("shared", "stamp", "\${this@missing:#{#{next} + '-' + '\${this@label}'}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("1-B", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun cacheExpressionsInsideDefaultsNestedInOtherExpressions() {
        property("shared", "stamp", "\${this@missing:#{\${this@alsoMissing:#{next}} + '-' + '\${this@label}'}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("1-B", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun resolveDefaultExpressionsBeforeUsingThemAsQuotedArguments() {
        val stamp = property("app", "stamp", "#{'\${this@missing:#{next}}'.length()}")

        assertEquals("1", stamp.resolveBy(resolver, context).value)
        assertEquals("1", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun preserveGeneratedPlaceholdersInsideDefaults() {
        property("shared", "stamp", "\${this@missing:#{next}-#{'$' + '{this@label}'}}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("1-B", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun retainContextualPlaceholdersWhileSharingIndependentExpressions() {
        property("shared", "stamp", "#{next}-\${this@label}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A", reference("first", "value"))
        assertEquals("1-B", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun decodeQuotedYamlBeforeEvaluatingCachedExpressions() {
        val stamp = property("app", "stamp",
            YamlScalars.doubleQuoted("#{next}-\${this@label}-#{'a\\b'}"), ValueFormat.YAML)
        property("app", "label", "app")

        assertEquals("1-app-a\\b", decoded(stamp.resolveBy(resolver, context).value, ValueFormat.YAML))
        assertEquals("1-app-a\\b", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun keepGeneratedPlaceholdersContextualInMixedProperties() {
        property("shared", "stamp", "#{next}-#{'$' + '{this@label}'}-\${this@name}")
        property("app", "name", "app")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")
        property("second", "label", "B")

        assertEquals("1-A-app", reference("first", "value"))
        assertEquals("1-B-app", reference("second", "value"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun keepRepeatedOccurrencesDistinctFromLazyDefaultExpressions() {
        property("shared", "stamp", "#{next}-\${this@label:#{next}}-#{next}")
        property("first", "value", "\${shared@stamp}")
        property("second", "value", "\${shared@stamp}")
        property("first", "label", "A")

        assertEquals("1-A-2", reference("first", "value"))
        assertEquals("1-3-2", reference("second", "value"))
        assertEquals(3, evaluations.get())
    }

    @Test
    fun avoidEvaluatingUnusedDefaultExpressions() {
        val stamp = property("app", "stamp", "#{next}-\${this@label:#{1 / 0}}")
        property("app", "label", "present")

        assertEquals("1-present", stamp.resolveBy(resolver, context).value)
        assertEquals("1-present", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun cacheNestedIndependentExpressions() {
        val stamp = property("app", "stamp", "#{#{next} + 10}-\${this@label}")
        property("app", "label", "app")

        assertEquals("11-app", stamp.resolveBy(resolver, context).value)
        assertEquals("11-app", reference("app", "stamp"))
        assertEquals(1, evaluations.get())
    }

    @Test
    fun keepIdenticalExpressionsInDifferentCollectionElementsDistinct() {
        val stamp = property("app", "stamp", "[#{next}, \${this@label}, #{next}]", ValueFormat.YAML)
        property("app", "label", "app")
        val expected = listOf(1, "app", 2)

        assertEquals(expected, Yaml().load<List<Any>>(stamp.resolveBy(resolver, context).value))
        assertEquals(expected, Yaml().load<List<Any>>(reference("app", "stamp")))
        assertEquals(2, evaluations.get())
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "#{next}-\${this@label}",
        "\${this@missing:#{next}}-\${this@label}",
        "\${this@missing:#{next + '-' + '\${this@label}'}}",
        "\${this@missing:#{#{next} + '-' + '\${this@label}'}}",
    ])
    fun evaluateMixedPropertyExpressionsOnceUnderConcurrentCacheMisses(value: String) {
        val stamp = property("app", "stamp", value)
        property("app", "label", "app")
        val pool = Executors.newFixedThreadPool(8)
        val ready = CountDownLatch(8)
        try {
            val tasks = (1..8).map { pool.submit<String> {
                ready.countDown()
                assertTrue(ready.await(5, SECONDS))
                stamp.resolveBy(resolver, context).value
            } }
            assertEquals(List(8) { "1-app" }, tasks.map { it.get(10, SECONDS) })
            assertEquals(1, evaluations.get())
        } finally {
            pool.shutdownNow()
        }
    }

    private fun property(component: String, key: String, value: String, format: ValueFormat = ValueFormat.TEXT): Property =
        PropertyImpl.property(key, value, if (format == ValueFormat.YAML) ConfigFormat.YAML else ConfigFormat.PROPERTIES,
            DeclaringComponentImpl("app", component, "dev"), format).also { properties[component to key] = it }

    private fun decoded(value: String, format: ValueFormat): String = when (format) {
        ValueFormat.YAML -> Yaml().load(value)
        ValueFormat.PROPERTIES -> PropertiesText.unescape(value)
        ValueFormat.TEXT -> value
    }

    private fun reference(component: String, key: String): String =
        resolver.resolve("\${$component@$key}", context, context)
}
