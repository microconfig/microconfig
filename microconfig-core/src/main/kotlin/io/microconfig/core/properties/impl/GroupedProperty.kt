package io.microconfig.core.properties.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.DeclaringComponent
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl
import io.microconfig.core.properties.Resolver
import io.microconfig.core.properties.ValueFormat
import io.microconfig.core.properties.io.properties.PropertiesText
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
                    relativeKey(it) to
                        YamlScalars.serializedValue(it)
                }
            )

    private fun relativeKey(property: Property): String {
        val relative =
            if (property.keyFormat != PROPERTIES || property.key.startsWith("$key."))
                property.key.substring(key.length + 1)
            else PropertiesText.unescape(property.key).substring(key.length + 1).replace("\\", "\\\\")
        return YamlScalars.serializedKey(property, relative)
    }

    override val isVar: Boolean
        get() = false

    override val configFormat: ConfigFormat
        get() = ConfigFormat.YAML

    override fun resolveBy(resolver: Resolver, root: DeclaringComponent): Property {
        val resolved = children.map { it.resolveBy(resolver, root) }
        return PropertyImpl.property(key, toYaml(resolved), ConfigFormat.YAML, declaringComponent,
            ValueFormat.YAML, keyFormat)
    }
}
