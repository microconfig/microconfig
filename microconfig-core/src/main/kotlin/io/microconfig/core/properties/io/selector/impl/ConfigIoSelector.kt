package io.microconfig.core.properties.io.selector.impl

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.core.properties.io.selector.ConfigFormatDetector
import java.io.File

class ConfigIoSelector(
    private val detector: ConfigFormatDetector,
    private val yaml: ConfigIo,
    private val properties: ConfigIo,
) : ConfigIo {
    override fun readFrom(file: File): ConfigReader = select(file).readFrom(file)

    override fun writeTo(file: File): ConfigWriter = select(file).writeTo(file)

    private fun select(file: File): ConfigIo =
        when (detector.detectConfigFormat(file)) {
            ConfigFormat.YAML -> yaml
            ConfigFormat.PROPERTIES -> properties
        }
}
