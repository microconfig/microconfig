package io.microconfig.core.properties.io.selector

import io.microconfig.core.properties.ConfigFormat
import java.io.File

fun interface ConfigFormatDetector {
    fun detectConfigFormat(file: File): ConfigFormat
}
