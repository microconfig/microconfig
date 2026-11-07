package io.microconfig.core.properties.resolvers.placeholder.impl

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.*
import io.microconfig.core.properties.io.properties.PropertiesText
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.expression.impl.ExpressionResolver
import io.microconfig.core.properties.resolvers.impl.MemoizingResolver
import io.microconfig.core.properties.resolvers.impl.PropertyResolutionCache
import io.microconfig.core.properties.resolvers.placeholder.Placeholder
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.*

class PlaceholderResolver
private constructor(
    private val environments: EnvironmentRepository,
    private val strategy: PlaceholderResolveStrategy,
    private val nonOverridableKeys: Set<String>,
    private val expressions: RecursiveResolver,
    private val cache: PropertyResolutionCache,
    private val visited: Set<Placeholder>,
    private val explicitEnvironment: String?,
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
        null,
    )

    internal constructor(
        environments: EnvironmentRepository,
        strategy: PlaceholderResolveStrategy,
        nonOverridableKeys: Set<String>,
        expressions: RecursiveResolver,
        cache: PropertyResolutionCache,
    ) : this(environments, strategy, nonOverridableKeys, expressions, cache, emptySet(), null)

    override fun findStatementIn(line: CharSequence): Optional<RecursiveResolver.Statement> =
        Optional.ofNullable(PlaceholderBorders.findPlaceholderOrNull(line)?.let(::PlaceholderStatement))

    private inner class PlaceholderStatement(private val borders: PlaceholderBorders) :
        RecursiveResolver.Statement {
        private var plainText = false
        private var literalText = false
        private var valueFormat = ValueFormat.TEXT
        override val startIndex: Int
            get() = borders.startIndex

        override val endIndex: Int
            get() = borders.endIndex

        override val isPlainText: Boolean
            get() = plainText

        override fun resolveFor(
            sourceOfValue: DeclaringComponent?,
            root: DeclaringComponent?,
        ): String = resolveFor(sourceOfValue, root, ValueFormat.TEXT)

        override fun resolveFor(
            sourceOfValue: DeclaringComponent?,
            root: DeclaringComponent?,
            targetFormat: ValueFormat,
        ): String {
            val source = requireNotNull(sourceOfValue)
            val context = requireNotNull(root)
            val placeholder = contextualPlaceholder(source, context)
            val value = resolveOrDefault(placeholder, source, rootFor(placeholder, context))
            return when {
                targetFormat == ValueFormat.YAML && literalText -> YamlScalars.literalValue(value)
                targetFormat == ValueFormat.TEXT && valueFormat == ValueFormat.YAML -> YamlScalars.unquote(value)
                else -> value
            }
        }

        private fun contextualPlaceholder(
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): Placeholder {
            val placeholder = borders.toPlaceholder(source.configType, source.environment)
            if (borders.hasExplicitEnvironment) return placeholder
            if (explicitEnvironment != null) return placeholder.withEnvironment(explicitEnvironment)
            return if (
                placeholder.environment != root.environment &&
                placeholder.environment in environments.profilesOf(root.environment)
            )
                placeholder.withEnvironment(root.environment)
            else placeholder
        }

        private fun rootFor(placeholder: Placeholder, root: DeclaringComponent): DeclaringComponent {
            val environment = explicitEnvironmentFor(placeholder) ?: return root
            return DeclaringComponentImpl(root.configType, root.component, environment)
        }

        private fun explicitEnvironmentFor(placeholder: Placeholder): String? =
            if (borders.hasExplicitEnvironment) placeholder.environment else explicitEnvironment

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
                if (default != null && error.isUnresolvedValue()) {
                    plainText = default.isEmpty()
                    literalText = false
                    valueFormat = ValueFormat.TEXT
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
            val environment = explicitEnvironmentFor(placeholder)
            val candidates =
                (listOf(root) + visited.map { it.referencedComponent } + source)
                    .map {
                        if (environment == null) DeclaringComponentImpl.copyOf(it)
                        else DeclaringComponentImpl(it.configType, it.component, environment)
                    }
                    .distinct()
            return candidates.firstNotNullOfOrNull {
                tryResolve(placeholder.overrideBy(it), source, root)
            } ?: throw ResolveException.unresolved(source, root, "Can't resolve placeholder $this")
        }

        private fun tryResolve(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String? =
            try {
                resolve(placeholder, source, root)
            } catch (error: RuntimeException) {
                if (error.isUnresolvedValue()) null else throw error
            }

        private fun resolve(
            placeholder: Placeholder,
            source: DeclaringComponent,
            root: DeclaringComponent,
        ): String {
            val property = placeholder.resolveUsing(strategy, root)
            val resolver =
                MemoizingResolver(
                    resolverWithVisited(placeholder),
                    cache,
                    expressions,
                    visited.toList() + placeholder,
                    explicitEnvironmentFor(placeholder),
                )
            val value = property.resolveBy(resolver, root).value
            plainText = property.valueFormat != ValueFormat.YAML && !property.isLiteralText
            literalText = property.isLiteralText
            valueFormat = property.valueFormat
            return decodedValue(value, property)
        }

        private fun resolverWithVisited(placeholder: Placeholder): PlaceholderResolver {
            if (placeholder in visited)
                throw UnresolvedValueException(
                    "Found cyclic dependencies:\n${visited.joinToString(" -> ")}"
                )
            return PlaceholderResolver(
                environments,
                strategy,
                nonOverridableKeys,
                expressions,
                cache,
                visited + placeholder,
                explicitEnvironmentFor(placeholder),
            )
        }

        private fun decodedValue(value: String, property: Property): String =
            when (property.valueFormat) {
                ValueFormat.PROPERTIES -> PropertiesText.unescape(value)
                else -> value
            }

        override fun toString(): String = borders.toString()
    }
}
