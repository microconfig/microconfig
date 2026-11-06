package io.microconfig.core.properties.io.selector

import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.properties.impl.PropertiesConfigIo
import io.microconfig.core.properties.io.selector.impl.ConfigFormatDetectorImpl
import io.microconfig.core.properties.io.selector.impl.ConfigIoSelector
import io.microconfig.core.properties.io.yaml.impl.YamlConfigIo
import io.microconfig.io.FsReader
import io.microconfig.io.impl.DumpedFsReader
import io.microconfig.utils.CacheProxy.cache

object ConfigIoFactory {
    private val instance = newConfigIo(DumpedFsReader())

    fun configIo(): ConfigIo = instance

    fun newConfigIo(reader: FsReader): ConfigIo =
        ConfigIoSelector(
            cache<ConfigFormatDetector>(ConfigFormatDetectorImpl(reader)),
            YamlConfigIo(reader),
            PropertiesConfigIo(reader),
        )
}
