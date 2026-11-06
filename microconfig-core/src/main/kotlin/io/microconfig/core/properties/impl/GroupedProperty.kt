package io.microconfig.core.properties.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.io.yaml.YamlTree

class GroupedProperty(
    override val key: String,
    private val children: Collection<Property>,
    override val declaringComponent: DeclaringComponent?,
) : Property {
    override val value: String
        get() = toYaml(children)

    private fun toYaml(properties: Collection<Property>): String =
        YamlTree.compact()
            .toYaml(
                properties.associate {
                    YamlScalars.serializedKey(it, it.key.substring(key.length + 1)) to
                        YamlScalars.serializedValue(it)
                }
            )

    override val isVar: Boolean
        get() = false

    override val configFormat: ConfigFormat
        get() = ConfigFormat.YAML

    override fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property {
        val resolved = children.map { it.resolveBy(resolver, root) }
        return PropertyImpl.property(key, toYaml(resolved), ConfigFormat.YAML, declaringComponent)
    }
}
