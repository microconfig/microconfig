package io.microconfig.core.properties.io.yaml.impl

import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.io.FsReader
import java.io.File

class YamlConfigIo(private val reader: FsReader) : ConfigIo {
    override fun readFrom(file: File): ConfigReader = SnakeYamlReader(file, reader)

    override fun writeTo(file: File): ConfigWriter = SnakeYamlWriter(file)
}
