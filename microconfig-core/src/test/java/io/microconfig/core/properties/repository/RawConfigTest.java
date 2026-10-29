package io.microconfig.core.properties.repository;

import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.OverrideProperty.overrideProperty;
import static io.microconfig.core.properties.PropertyImpl.property;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawConfigTest {
    @ParameterizedTest
    @CsvSource({"p1, p2, 300", "p2, p1, 200"})
    void applyProfileOverridesInProfileOrder(String first, String second, String expected) {
        RawConfig config = configWithProfileOverrides();

        assertEquals(expected, timeoutOf(config, asList(first, second)));
    }

    @ParameterizedTest
    @CsvSource({"p1, p2", "p2, p1"})
    void environmentOverridesAllProfiles(String first, String second) {
        RawConfig config = configWithProfileOverrides(overrideProperty("+dev.timeout", "400", YAML, null));

        assertEquals("400", timeoutOf(config, asList(first, second)));
    }

    @Test
    void ignoreInactiveProfileOverrides() {
        RawConfig config = configWithProfileOverrides();

        assertEquals("200", timeoutOf(config, asList("p1")));
    }

    @Test
    void keepBaseValueWithoutActiveProfiles() {
        RawConfig config = configWithProfileOverrides();

        assertEquals("100", timeoutOf(config, emptyList()));
    }

    @Test
    void rejectDuplicateOverridesWithinSameProfile() {
        RawConfig config = configWithProfileOverrides(overrideProperty("+p1.timeout", "999", YAML, null));

        assertThrows(IllegalStateException.class, () -> timeoutOf(config, asList("p1")));
    }

    private RawConfig configWithProfileOverrides(Property... additionalProperties) {
        List<Property> properties = new ArrayList<>(asList(
                property("timeout", "100", YAML, null),
                overrideProperty("+p2.timeout", "300", YAML, null),
                overrideProperty("+p1.timeout", "200", YAML, null),
                overrideProperty("+inactive.timeout", "999", YAML, null)
        ));
        properties.addAll(asList(additionalProperties));
        return new RawConfig(emptyList(), properties);
    }

    private String timeoutOf(RawConfig config, List<String> profiles) {
        return config.getBaseAndIncludedProperties(include -> emptyMap(), profiles, "dev")
                .get("timeout").getValue();
    }
}
