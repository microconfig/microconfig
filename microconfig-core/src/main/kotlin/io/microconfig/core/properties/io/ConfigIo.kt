package io.microconfig.core.properties.io

import java.io.File

interface ConfigIo {
    fun readFrom(file: File): ConfigReader

    fun writeTo(file: File): ConfigWriter
}
