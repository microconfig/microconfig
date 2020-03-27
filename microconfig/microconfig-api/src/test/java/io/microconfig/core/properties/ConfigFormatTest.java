package io.microconfig.core.properties;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigFormatTest {
    @Test
    void extension() {
        assertEquals(".yaml", YAML.extension());
        assertEquals(".properties", PROPERTIES.extension());
    }
}