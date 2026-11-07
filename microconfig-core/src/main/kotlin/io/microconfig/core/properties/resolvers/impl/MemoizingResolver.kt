package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.resolvers.placeholder.Placeholder

internal class MemoizingResolver(
    private val delegate: Resolver,
    private val cache: PropertyResolutionCache,
    private val referenceChain: List<Placeholder> = emptyList(),
) : Resolver {
    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String = delegate.resolve(value, sourceOfValue, root)

    override fun resolve(property: Property, root: DeclaringComponent): String =
        cache.resolve(property, root, delegate, referenceChain)
}
