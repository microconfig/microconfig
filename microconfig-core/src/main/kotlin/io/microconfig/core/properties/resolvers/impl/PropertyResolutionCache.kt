package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.placeholder.Placeholder
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

internal class PropertyResolutionCache {
    private val values = ConcurrentHashMap<Key, String>()
    private val expressionValues = ConcurrentHashMap<Pair<Key, Int>, String>()

    fun resolve(
        property: Property,
        root: DeclaringComponent,
        placeholders: RecursiveResolver,
        expressions: RecursiveResolver,
        referenceChain: List<Placeholder> = emptyList(),
        explicitEnvironment: String? = null,
    ): String {
        val key = keyFor(property, root, referenceChain, explicitEnvironment)
        return values.getOrPut(key) {
            val cachedExpressions = expressionsFor(key, expressions)
            YamlResolvers.forValues(placeholders, cachedExpressions).resolve(property, root)
        }
    }

    private fun keyFor(
        property: Property,
        root: DeclaringComponent,
        referenceChain: List<Placeholder>,
        explicitEnvironment: String?,
    ): Key {
        return Key(
            PropertyImpl(
                property.key,
                property.value,
                property.isVar,
                property.configFormat,
                property.declaringComponent,
                property.isLiteralText,
                property.valueFormat,
                property.keyFormat,
            ),
            DeclaringComponentImpl.copyOf(root),
            referenceChain,
            explicitEnvironment,
        )
    }

    private fun expressionsFor(key: Key, expressions: RecursiveResolver): RecursiveResolver {
        if (
            PlaceholderBorders.findPlaceholderOrNull(key.property.value) != null ||
                expressions.findStatementIn(key.property.value).getOrNull() == null
        ) return expressions
        val expressionKey = key.copy(referenceChain = emptyList(), explicitEnvironment = null)
        var index = 0
        var independent = true
        return object : RecursiveResolver {
            override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
                Optional.ofNullable(expressions.findStatementIn(line).getOrNull()?.let { statement ->
                    object : RecursiveResolver.Statement by statement {
                        override fun resolveFor(
                            sourceOfValue: DeclaringComponent?,
                            root: DeclaringComponent?,
                            targetFormat: ValueFormat,
                        ): String {
                            if (!independent) return statement.resolveFor(sourceOfValue, root, targetFormat)
                            val value = expressionValues.getOrPut(expressionKey to index++) {
                                statement.resolveFor(sourceOfValue, root, targetFormat)
                            }
                            // Later expressions may depend on the generated placeholder's context.
                            if (PlaceholderBorders.findPlaceholderOrNull(value) != null) independent = false
                            return value
                        }
                    }
                })
        }
    }

    private data class Key(
        val property: PropertyImpl,
        val root: DeclaringComponent,
        val referenceChain: List<Placeholder>,
        val explicitEnvironment: String?,
    )
}
