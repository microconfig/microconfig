package io.microconfig.core.properties

import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.ConfigFormat.YAML
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigFormatTest {
    @Test
    fun extension() {
        assertEquals(".yaml", YAML.extension())
        assertEquals(".properties", PROPERTIES.extension())
    }
}
