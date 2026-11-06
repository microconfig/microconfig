package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.Property
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.core.properties.io.yaml.YamlScalars
import io.microconfig.core.properties.io.yaml.YamlTree
import io.microconfig.utils.FileUtils
import java.io.File

class SnakeYamlWriter(private val file: File) : ConfigWriter {
    override fun write(properties: Map<String, String>) =
        writeText(YamlTree.standard().toYaml(properties))

    private fun writeText(text: String) = FileUtils.write(file, text)

    override fun write(properties: Collection<Property>) = writeText(serialize(properties))

    override fun serialize(properties: Collection<Property>): String =
        YamlTree.standard()
            .toYaml(
                properties
                    .filterNot(Property::isVar)
                    .associate { YamlScalars.serializedKey(it) to serializedValue(it) }
                    .toSortedMap()
            )

    override fun serializedValue(property: Property): String =
        SnakeYaml.fragment(SnakeYaml.parse(YamlScalars.serializedValue(property)))
}
