package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.io.yaml.YamlResolvers
import io.microconfig.core.properties.resolvers.RecursiveResolver
import io.microconfig.core.properties.resolvers.placeholder.Placeholder

internal class MemoizingResolver(
    private val placeholders: RecursiveResolver,
    private val cache: PropertyResolutionCache,
    private val expressions: RecursiveResolver,
    private val referenceChain: List<Placeholder> = emptyList(),
    private val explicitEnvironment: String? = null,
) : Resolver {
    private val delegate = YamlResolvers.forValues(placeholders, expressions)

    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String = delegate.resolve(value, sourceOfValue, root)

    override fun resolve(property: Property, root: DeclaringComponent): String =
        cache.resolve(property, root, placeholders, expressions, referenceChain, explicitEnvironment)
}
