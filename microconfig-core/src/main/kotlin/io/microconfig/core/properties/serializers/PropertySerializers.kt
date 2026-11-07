package io.microconfig.core.properties.serializers

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertySerializer
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo as defaultConfigIo
import io.microconfig.core.properties.serializers.impl.ConfigDiff
import io.microconfig.core.properties.serializers.impl.FilePropertySerializer
import java.io.File
import java.util.function.BiConsumer

object PropertySerializers {
    @JvmOverloads
    fun withConfigDiff(
        configIo: ConfigIo = defaultConfigIo()
    ): BiConsumer<File, Collection<Property>> = ConfigDiff(configIo)

    @JvmOverloads
    fun toFileIn(
        dir: File,
        listener: BiConsumer<File, Collection<Property>>,
        configIo: ConfigIo = defaultConfigIo(),
    ): PropertySerializer<File> = FilePropertySerializer(dir, listener, configIo)

    @JvmOverloads
    fun asString(configIo: ConfigIo = defaultConfigIo()): PropertySerializer<String> =
        PropertySerializer { properties, _, _, _, _ ->
            configIo
                .writeTo(File(extensionByConfigFormat(properties).extension()))
                .serialize(properties)
        }

    private fun extensionByConfigFormat(properties: Collection<Property>): ConfigFormat =
        if (properties.isEmpty() || properties.any { it.configFormat == ConfigFormat.YAML })
            ConfigFormat.YAML
        else ConfigFormat.PROPERTIES
}
