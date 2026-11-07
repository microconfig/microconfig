package io.microconfig.core.properties.resolvers.impl

import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.DeclaringComponentImpl
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.resolvers.placeholder.Placeholder
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderBorders
import java.util.concurrent.ConcurrentHashMap

internal class PropertyResolutionCache {
    private val values = ConcurrentHashMap<Key, String>()

    fun resolve(
        property: Property,
        root: DeclaringComponent,
        resolver: Resolver,
        referenceChain: List<Placeholder> = emptyList(),
    ): String =
        values.getOrPut(keyFor(property, root, referenceChain)) {
            resolver.resolve(property, root)
        }

    private fun keyFor(
        property: Property,
        root: DeclaringComponent,
        referenceChain: List<Placeholder>,
    ): Key =
        Key(
            PropertyImpl(
                property.key,
                property.value,
                property.isVar,
                property.configFormat,
                property.declaringComponent,
                property.isLiteralText,
            ),
            DeclaringComponentImpl.copyOf(root),
            if (PlaceholderBorders.findPlaceholderIn(property.value).isPresent) referenceChain
            else emptyList(),
        )

    private data class Key(
        val property: Property,
        val root: DeclaringComponent,
        val referenceChain: List<Placeholder>,
    )
}
