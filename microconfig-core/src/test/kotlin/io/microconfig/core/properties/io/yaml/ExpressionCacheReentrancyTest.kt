package io.microconfig.core.properties.io.yaml

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.placeholder.impl.PlaceholderResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import java.util.Optional

class ExpressionCacheReentrancyTest {
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun resolveGeneratedPlaceholdersBeforeComputingCachedExpressions(inDefault: Boolean) {
        val expression = "#{#{'$' + '{target@operand}'} + 1}"
        val value = (if (inDefault) "\${this@missing:$expression}" else expression) + "-\${this@label}"

        assertEquals("3-app", resolveWithCacheBoundaryGuard(value))
    }

    @ParameterizedTest
    @EnumSource(ValueFormat::class)
    fun resolveWholeGeneratedPlaceholdersOutsideTheCache(format: ValueFormat) {
        val value = "\${this@missing:#{\${this@code}}}-\${this@label}"

        assertEquals("2-app", resolveWithCacheBoundaryGuard(value, format))
    }

    private fun resolveWithCacheBoundaryGuard(value: String, format: ValueFormat = ValueFormat.TEXT): String {
        val context = DeclaringComponentImpl("app", "app", "dev")
        var computing = false
        val cache = ExpressionValueCache { _, _, _, evaluate ->
            assertFalse(computing, "Expression computation reentered the cache")
            computing = true
            try {
                evaluate()
            } finally {
                computing = false
            }
        }
        val strategy = PlaceholderResolveStrategy { _, key, _, _, _ ->
            assertFalse(computing, "Placeholder lookup ran inside a cache computation")
            val value = when (key) {
                "operand" -> "2"
                "label" -> "app"
                "code" -> "'$' + '{target@operand'}"
                else -> null
            }
            Optional.ofNullable(value?.let { PropertyImpl.property(key, it, YAML, context, ValueFormat.TEXT) })
        }
        val placeholders = PlaceholderResolver(mock(EnvironmentRepository::class.java), strategy, emptySet())
        val resolver = YamlResolvers.withExpressionCache(cache, placeholders, ExpressionResolver())
        val property = PropertyImpl.property("value", value, YAML, context, format)

        return property.resolveBy(resolver, context).value
    }
}
