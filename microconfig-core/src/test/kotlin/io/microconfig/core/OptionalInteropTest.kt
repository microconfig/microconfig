package io.microconfig.core

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.TypedProperties
import io.microconfig.core.properties.impl.PropertiesImpl
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.impl.ChainedResolver
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import io.microconfig.core.properties.resolvers.placeholder.strategies.composite.impl.CompositeResolveStrategy
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class OptionalInteropTest {
    @Test
    fun keepOptionalAndNullableParserResultsConsistent() {
        val text = "before \${component@key:fallback} after"
        val nullable = PlaceholderBorders.findPlaceholderOrNull(text)
        val optional = PlaceholderBorders.findPlaceholderIn(text)

        assertNotNull(nullable)
        assertTrue(optional.isPresent)
        assertEquals(optional.orElseThrow().toString(), nullable.toString())
        assertEquals(optional.orElseThrow().startIndex, nullable!!.startIndex)
        assertEquals(optional.orElseThrow().endIndex, nullable.endIndex)
        assertNull(PlaceholderBorders.findPlaceholderOrNull("plain"))
        assertTrue(PlaceholderBorders.findPlaceholderIn("plain").isEmpty)
    }

    @Test
    fun findPropertiesThroughCustomOptionalProvidersWithoutVisitingLaterProviders() {
        val empty = mock(TypedProperties::class.java)
        val present = mock(TypedProperties::class.java)
        val unvisited = mock(TypedProperties::class.java)
        val property = PropertyImpl.property("key", "value", ConfigFormat.YAML, null)
        `when`(empty.getPropertyWithKey("key")).thenReturn(Optional.empty())
        `when`(present.getPropertyWithKey("key")).thenReturn(Optional.of(property))
        val properties = PropertiesImpl(listOf(empty, present, unvisited))

        assertSame(property, properties.getPropertyWithKey("key").getOrNull())
        verifyNoInteractions(unvisited)
        assertNull(PropertiesImpl(emptyList()).getPropertyWithKey("key").getOrNull())
    }

    @Test
    fun preserveCustomStrategyPresenceAndFailures() {
        val property = PropertyImpl.property("key", "value", ConfigFormat.YAML, null)
        var laterCalls = 0
        val failure = IllegalArgumentException("custom failure")
        val empty = PlaceholderResolveStrategy { _, _, _, _, _ -> Optional.empty() }
        val present = PlaceholderResolveStrategy { _, key, _, _, _ ->
            if (key == "key") Optional.of(property) else Optional.empty()
        }
        val throwing = PlaceholderResolveStrategy { _, _, _, _, _ -> laterCalls++; throw failure }

        val result = CompositeResolveStrategy(listOf(empty, present, throwing))
            .resolve("component", "key", "dev", "app", null)
        assertSame(property, result.getOrNull())
        assertEquals(0, laterCalls)
        assertNull(CompositeResolveStrategy(listOf(empty, present))
            .resolve("component", "absent", "dev", "app", null).getOrNull())
        val actual = assertThrows(IllegalArgumentException::class.java) {
            CompositeResolveStrategy(listOf(empty, throwing))
                .resolve("component", "key", "dev", "app", null)
        }
        assertSame(failure, actual)
    }

    @Test
    fun preserveCustomRecursiveResolverAndItsErrors() {
        val failure = IllegalStateException("custom scanner failure")
        val custom = object : RecursiveResolver {
            override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> {
                if (line.toString() == "broken") throw failure
                val start = line.indexOf("token")
                return Optional.ofNullable(if (start < 0) null else object : RecursiveResolver.Statement {
                    override val startIndex = start
                    override val endIndex = start + "token".length
                    override fun resolveFor(sourceOfValue: DeclaringComponent?, root: DeclaringComponent?) = "resolved"
                })
            }
        }
        val chain = ChainedResolver.recursiveChainOf(custom)

        assertNotNull(chain.findStatementIn("token").getOrNull())
        assertNull(chain.findStatementIn("plain").getOrNull())
        assertEquals("before resolved after", chain.resolve("before token after", null, null))
        assertSame(failure, assertThrows(IllegalStateException::class.java) { chain.findStatementIn("broken") })

        val root = DeclaringComponentImpl("app", "component", "dev")
        val yaml = YamlResolvers.forValues(custom)
        assertEquals("resolved", yaml.resolve(
            PropertyImpl.property("key", "token", ConfigFormat.YAML, root), root))
        assertSame(failure, assertThrows(IllegalStateException::class.java) {
            yaml.resolve(PropertyImpl.property("key", "broken", ConfigFormat.YAML, root), root)
        })
    }
}
