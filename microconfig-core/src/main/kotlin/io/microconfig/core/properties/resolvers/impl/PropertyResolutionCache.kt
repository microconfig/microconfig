package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.*
import io.microconfig.core.properties.io.yaml.ExpressionValueCache
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.placeholder.Placeholder
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

internal class PropertyResolutionCache {
    private val values = ConcurrentHashMap<Key, String>()
    private val expressionValues = ConcurrentHashMap<Pair<Key, Int>, String>()
    private val contextualExpressionValues = ConcurrentHashMap<ContextualExpression, String>()
    private val independentValues = ConcurrentHashMap<IndependentExpression, String>()

    fun resolve(
        property: Property,
        root: DeclaringComponent,
        placeholders: RecursiveResolver,
        expressions: RecursiveResolver,
        referenceChain: List<Placeholder> = emptyList(),
        explicitEnvironment: String? = null,
    ): String {
        val value = property.value
        if ((property.valueFormat != ValueFormat.YAML || YamlScalars.isPlainScalar(value)) &&
            placeholders.findStatementIn(value).isEmpty && expressions.findStatementIn(value).isEmpty
        )
            return value
        val key = keyFor(property, root, referenceChain, explicitEnvironment)
        return values.getOrPut(key) {
            resolverFor(key, placeholders, expressions).resolve(property, root)
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

    private fun resolverFor(key: Key, placeholders: RecursiveResolver, expressions: RecursiveResolver) =
        when {
            expressions.findStatementIn(key.property.value).isEmpty ->
                YamlResolvers.forValues(placeholders, expressions)

            PlaceholderBorders.findPlaceholderOrNull(key.property.value) == null ->
                expressionOnlyResolver(key, placeholders, expressions)

            else -> YamlResolvers.withExpressionCache(independentExpressionsFor(key), placeholders, expressions)
        }

    private fun expressionOnlyResolver(
        key: Key,
        placeholders: RecursiveResolver,
        expressions: RecursiveResolver,
    ): Resolver {
        val resolvedPlaceholders = mutableListOf<Pair<String, String>>()
        return YamlResolvers.forValues(
            trackingPlaceholders(placeholders, resolvedPlaceholders),
            expressionsFor(key, expressions, resolvedPlaceholders),
        )
    }

    private fun trackingPlaceholders(
        placeholders: RecursiveResolver,
        resolved: MutableList<Pair<String, String>>,
    ): RecursiveResolver = object : RecursiveResolver {
        override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
            placeholders.findStatementIn(line).map { statement ->
                val placeholder = line.subSequence(statement.startIndex, statement.endIndex).toString()
                object : RecursiveResolver.Statement by statement {
                    override fun resolveFor(
                        sourceOfValue: DeclaringComponent?,
                        root: DeclaringComponent?,
                        targetFormat: ValueFormat,
                    ): String = statement.resolveFor(sourceOfValue, root, targetFormat).also {
                        resolved.add(placeholder to it)
                    }
                }
            }
    }

    private fun expressionsFor(
        key: Key,
        expressions: RecursiveResolver,
        resolvedPlaceholders: List<Pair<String, String>>,
    ): RecursiveResolver {
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
                            val position = index++
                            val evaluate = { statement.resolveFor(sourceOfValue, root, targetFormat) }
                            val value =
                                if (independent) expressionValues.computeIfAbsent(expressionKey to position) { evaluate() }
                                else contextualExpressionValues.computeIfAbsent(
                                    ContextualExpression(expressionKey, position, line.toString(), resolvedPlaceholders.toList())
                                ) { evaluate() }
                            // Share later evaluations only when generated placeholders resolved to the same values.
                            if (PlaceholderBorders.findPlaceholderOrNull(value) != null) independent = false
                            return value
                        }
                    }
                })
        }
    }

    private fun independentExpressionsFor(key: Key): ExpressionValueCache {
        val expressionKey = key.copy(referenceChain = emptyList(), explicitEnvironment = null)
        return ExpressionValueCache { scalarStart, expressionStart, expression, evaluate ->
            independentValues.computeIfAbsent(
                IndependentExpression(expressionKey, scalarStart, expressionStart, expression)
            ) { evaluate() }
        }
    }

    private data class ContextualExpression(
        val property: Key,
        val position: Int,
        val expandedValue: String,
        val resolvedPlaceholders: List<Pair<String, String>>,
    )

    private data class IndependentExpression(
        val property: Key,
        val scalarStart: Int,
        val expressionStart: Int,
        val expression: String,
    )

    private data class Key(
        val property: PropertyImpl,
        val root: DeclaringComponent,
        val referenceChain: List<Placeholder>,
        val explicitEnvironment: String?,
    )
}
