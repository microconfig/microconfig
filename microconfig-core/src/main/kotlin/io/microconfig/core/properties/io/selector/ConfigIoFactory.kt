package io.microconfig.core.properties.io.selector

import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.properties.impl.PropertiesConfigIo
import io.microconfig.core.properties.io.selector.impl.ConfigFormatDetectorImpl
import io.microconfig.core.properties.io.selector.impl.ConfigIoSelector
import io.microconfig.core.properties.io.yaml.impl.SnakeYamlReader
import io.microconfig.core.properties.io.yaml.impl.YamlConfigIo
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import java.io.File

object ConfigIoFactory {
    fun configIo(): ConfigIo = newConfigIo(DumpedFsReader())

    internal fun readYamlText(file: File, text: String): ConfigReader = SnakeYamlReader(file, text)

    fun newConfigIo(reader: FsReader): ConfigIo =
        ConfigIoSelector(
            ConfigFormatDetectorImpl(reader),
            YamlConfigIo(reader),
            PropertiesConfigIo(reader),
        )
}
