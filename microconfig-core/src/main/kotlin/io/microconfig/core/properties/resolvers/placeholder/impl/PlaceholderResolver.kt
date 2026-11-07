package io.microconfig.core.properties.resolvers.placeholder.impl

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.FileBasedComponent
import io.microconfig.core.properties.PlaceholderResolveStrategy
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.ResolveException
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.impl.MemoizingResolver
import io.microconfig.core.properties.resolvers.impl.PropertyResolutionCache
import io.microconfig.core.properties.resolvers.placeholder.Placeholder
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.Optional

class PlaceholderResolver
private constructor(
    private val environments: EnvironmentRepository,
    private val strategy: PlaceholderResolveStrategy,
    private val nonOverridableKeys: Set<String>,
    private val expressions: RecursiveResolver,
    private val cache: PropertyResolutionCache,
    private val visited: Set<Placeholder>,
) : RecursiveResolver {
    constructor(
        environments: EnvironmentRepository,
        strategy: PlaceholderResolveStrategy,
        nonOverridableKeys: Set<String>,
    ) : this(
        environments,
        strategy,
        nonOverridableKeys,
        ExpressionResolver(),
        PropertyResolutionCache(),
        emptySet(),
    )

    internal constructor(
        environments: EnvironmentRepository,
        strategy: PlaceholderResolveStrategy,
        nonOverridableKeys: Set<String>,
        expressions: RecursiveResolver,
        cache: PropertyResolutionCache,
    ) : this(environments, strategy, nonOverridableKeys, expressions, cache, emptySet())

    override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
        PlaceholderBorders.findPlaceholderIn(line).map(::PlaceholderStatement)

    private inner class PlaceholderStatement(private val borders: PlaceholderBorders) :
        RecursiveResolver.Statement {
        private var plainText = false
        override val startIndex: Int
            get() = borders.startIndex

        override val endIndex: Int
            get() = borders.endIndex

        override val isPlainText: Boolean
            get() = plainText

        override fun resolveFor(
            sourceOfValue: DeclaringComponent?,
            root: DeclaringComponent?,
        ): String {
            val source = requireNotNull(sourceOfValue)
            val context = requireNotNull(root)
            val placeholder = contextualPlaceholder(source, context)
            return resolveOrDefault(placeholder, source, context)
        }

        private fun contextualPlaceholder(
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): Placeholder {
            val placeholder = borders.toPlaceholder(source.configType, source.environment)
            return if (
                placeholder.environment != root.environment &&
                    placeholder.environment in environments.profilesOf(root.environment)
            )
                placeholder.withEnvironment(root.environment)
            else placeholder
        }

        private fun resolveOrDefault(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String =
            try {
                if (canBeOverridden(placeholder, source))
                    overrideByParents(placeholder, source, root)
                else resolve(placeholder, source, root)
            } catch (error: RuntimeException) {
                val default = placeholder.defaultValue
                if (default != null) {
                    plainText = default.isEmpty()
                    default
                } else throw ResolveException(source, root, "Can't resolve $this", error)
            }

        private fun canBeOverridden(placeholder: Placeholder, source: DeclaringComponent): Boolean =
            placeholder.isSelfReferenced ||
                placeholder.referencedTo(source) && placeholder.key !in nonOverridableKeys

        private fun overrideByParents(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String {
            val candidates =
                (listOf(root) + visited.map { it.referencedComponent } + source)
                    .map(DeclaringComponentImpl::copyOf)
                    .distinct()
            return candidates.firstNotNullOfOrNull {
                tryResolve(placeholder.overrideBy(it), source, root)
            } ?: throw ResolveException(source, root, "Can't resolve placeholder $this")
        }

        private fun tryResolve(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String? =
            try {
                resolve(placeholder, source, root)
            } catch (_: RuntimeException) {
                null
            }

        private fun resolve(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String {
            val property = placeholder.resolveUsing(strategy, root)
            val resolver =
                MemoizingResolver(
                    YamlResolvers.forValues(resolverWithVisited(placeholder), expressions),
                    cache,
                    visited.toList() + placeholder,
                )
            val value = property.resolveBy(resolver, root).value
            plainText = property.configFormat != YAML
            return convertForTarget(value, property, source)
        }

        private fun resolverWithVisited(placeholder: Placeholder): PlaceholderResolver {
            check(placeholder !in visited) {
                "Found cyclic dependencies:\n${visited.joinToString(" -> ")}"
            }
            return PlaceholderResolver(
                environments,
                strategy,
                nonOverridableKeys,
                expressions,
                cache,
                visited + placeholder,
            )
        }

        private fun convertForTarget(
            value: String,
            inserted: Property,
            target: DeclaringComponent,
        ): String {
            val fromProperties = PropertiesText.isPropertiesText(inserted.declaringComponent)
            val toProperties = PropertiesText.isPropertiesText(target)
            if (fromProperties && toProperties) return value
            val yamlTarget = target is FileBasedComponent && target.isYaml
            if (inserted.isLiteralText) return literalForTarget(value, yamlTarget, toProperties)
            val plain =
                when {
                    fromProperties -> PropertiesText.unescape(value)
                    !yamlTarget && inserted.configFormat == YAML && YamlScalars.isQuoted(value) ->
                        YamlScalars.unquote(value)
                    else -> value
                }
            return if (toProperties) PropertiesText.escape(plain) else plain
        }

        private fun literalForTarget(value: String, yaml: Boolean, properties: Boolean): String =
            when {
                yaml -> YamlScalars.literalValue(value)
                properties -> PropertiesText.escape(value)
                else -> value
            }

        override fun toString(): String = borders.toString()
    }
}
