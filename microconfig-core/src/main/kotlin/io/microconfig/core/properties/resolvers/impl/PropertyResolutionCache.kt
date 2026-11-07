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
        explicitEnvironment: String? = null,
    ): String =
        values.getOrPut(keyFor(property, root, referenceChain, explicitEnvironment)) {
            resolver.resolve(property, root)
        }

    private fun keyFor(
        property: Property,
        root: DeclaringComponent,
        referenceChain: List<Placeholder>,
        explicitEnvironment: String?,
    ): Key {
        val hasPlaceholders = PlaceholderBorders.findPlaceholderIn(property.value).isPresent
        return Key(
            PropertyImpl(
                property.key,
                property.value,
                property.isVar,
                property.configFormat,
                property.declaringComponent,
                property.isLiteralText,
            ),
            DeclaringComponentImpl.copyOf(root),
            if (hasPlaceholders) referenceChain else emptyList(),
            if (hasPlaceholders) explicitEnvironment else null,
        )
    }

    private data class Key(
        val property: Property,
        val root: DeclaringComponent,
        val referenceChain: List<Placeholder>,
        val explicitEnvironment: String?,
    )
}
