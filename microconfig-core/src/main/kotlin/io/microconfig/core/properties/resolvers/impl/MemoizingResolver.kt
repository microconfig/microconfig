package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.Resolver

internal class MemoizingResolver(
    private val delegate: Resolver,
    private val cache: PropertyResolutionCache,
) : Resolver {
    override fun resolve(
        value: String,
        sourceOfValue: DeclaringComponent?,
        root: DeclaringComponent?,
    ): String = delegate.resolve(value, sourceOfValue, root)

    override fun resolve(property: Property, root: DeclaringComponent): String =
        cache.resolve(property, root, delegate)
}
