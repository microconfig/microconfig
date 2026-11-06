package io.microconfig.core.properties.io

import io.microconfig.core.properties.ConfigFormat
import io.microconfig.core.properties.Property

interface ConfigReader {
    fun properties(configType: String, environment: String): List<Property>

    fun propertiesAsMap(): Map<String, String>

    fun commentsByLineNumber(): Map<Int, String>

    fun format(): ConfigFormat
}
