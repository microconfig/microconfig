package io.microconfig.core.properties.io.selector

import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.properties.impl.PropertiesConfigIo
import io.microconfig.core.properties.io.selector.impl.ConfigFormatDetectorImpl
import io.microconfig.core.properties.io.selector.impl.ConfigIoSelector
import io.microconfig.core.properties.io.yaml.impl.YamlConfigIo
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader

object ConfigIoFactory {
    fun configIo(): ConfigIo = newConfigIo(DumpedFsReader())

    fun newConfigIo(reader: FsReader): ConfigIo =
        ConfigIoSelector(
            ConfigFormatDetectorImpl(reader),
            YamlConfigIo(reader),
            PropertiesConfigIo(reader),
        )
}
