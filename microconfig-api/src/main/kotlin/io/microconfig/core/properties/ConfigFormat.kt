package io.microconfig.core.properties

import java.util.Locale
import java.util.Locale.ROOT

enum class ConfigFormat {
    YAML,
    PROPERTIES;

    fun extension(): String = "." + name.lowercase(ROOT)
}
