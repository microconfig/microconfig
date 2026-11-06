package io.microconfig.core.properties

import java.util.Locale

enum class ConfigFormat {
    YAML,
    PROPERTIES;

    fun extension(): String = "." + name.lowercase(Locale.ROOT)
}
