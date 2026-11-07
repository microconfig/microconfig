package io.microconfig.core.properties.serializers

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.serializers.impl.ConfigDiff
import io.microconfig.core.properties.serializers.impl.FilePropertySerializer
import java.io.File
import java.util.function.BiConsumer

object PropertySerializers {
    fun withConfigDiff(): BiConsumer<File, Collection<Property>> = ConfigDiff()

    fun toFileIn(
        dir: File,
        listener: BiConsumer<File, Collection<Property>>,
    ): PropertySerializer<File> = FilePropertySerializer(dir, listener)

    fun asString(): PropertySerializer<String> = PropertySerializer { properties, _, _, _, _ ->
        configIo()
            .writeTo(File(extensionByConfigFormat(properties).extension()))
            .serialize(properties)
    }

    private fun extensionByConfigFormat(properties: Collection<Property>): ConfigFormat =
        if (properties.isEmpty() || properties.any { it.configFormat == ConfigFormat.YAML })
            ConfigFormat.YAML
        else ConfigFormat.PROPERTIES
}
