package io.microconfig.core.properties.io.properties.impl

import io.microconfig.core.properties.io.ConfigIo
import io.microconfig.core.properties.io.ConfigReader
import io.microconfig.core.properties.io.ConfigWriter
import io.microconfig.io.FsReader
import java.io.File

class PropertiesConfigIo(private val reader: FsReader) : ConfigIo {
    override fun readFrom(file: File): ConfigReader = PropertiesReader(file, reader)

    override fun writeTo(file: File): ConfigWriter = PropertiesWriter(file)
}
