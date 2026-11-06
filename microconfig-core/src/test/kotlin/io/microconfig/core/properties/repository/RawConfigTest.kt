package io.microconfig.core.properties.repository

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.OverrideProperty.Companion.overrideProperty
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.property
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RawConfigTest {
    @ParameterizedTest
    @CsvSource(value = ["p1, p2, 300", "p2, p1, 200"])
    fun applyProfileOverridesInProfileOrder(first: String, second: String, expected: String) {
        val config: RawConfig = configWithProfileOverrides()
        assertEquals(expected, timeoutOf(config, listOf(first, second)))
    }

    @ParameterizedTest
    @CsvSource(value = ["p1, p2", "p2, p1"])
    fun environmentOverridesAllProfiles(first: String, second: String) {
        val config: RawConfig =
            configWithProfileOverrides(overrideProperty("+dev.timeout", "400", YAML, null))
        assertEquals("400", timeoutOf(config, listOf(first, second)))
    }

    @Test
    fun ignoreInactiveProfileOverrides() {
        val config: RawConfig = configWithProfileOverrides()
        assertEquals("200", timeoutOf(config, listOf("p1")))
    }

    @Test
    fun keepBaseValueWithoutActiveProfiles() {
        val config: RawConfig = configWithProfileOverrides()
        assertEquals("100", timeoutOf(config, emptyList()))
    }

    @Test
    fun rejectDuplicateOverridesWithinSameProfile() {
        val config: RawConfig =
            configWithProfileOverrides(overrideProperty("+p1.timeout", "999", YAML, null))
        assertThrows(IllegalStateException::class.java, { timeoutOf(config, listOf("p1")) })
    }

    @ParameterizedTest
    @CsvSource(value = ["\'+dev,prod.timeout\'", "\'+prod, dev.timeout\'"])
    fun applyOverrideSharedBetweenEnvs(key: String) {
        val config: RawConfig = configWithProfileOverrides(overrideProperty(key, "500", YAML, null))
        assertEquals("500", timeoutOf(config, emptyList()))
    }

    @Test
    fun ignoreSharedOverrideForOtherEnvs() {
        val config: RawConfig =
            configWithProfileOverrides(overrideProperty("+prod,uat.timeout", "500", YAML, null))
        assertEquals("100", timeoutOf(config, emptyList()))
    }

    @Test
    fun singleEnvOverrideWinsOverSharedOneDeclaredAfter() {
        val config: RawConfig =
            configWithProfileOverrides(
                overrideProperty("+dev.timeout", "400", YAML, null),
                overrideProperty("+dev,prod.timeout", "500", YAML, null),
            )
        assertEquals("400", timeoutOf(config, emptyList()))
    }

    @Test
    fun singleEnvOverrideWinsOverSharedOneDeclaredBefore() {
        val config: RawConfig =
            configWithProfileOverrides(
                overrideProperty("+dev,prod.timeout", "500", YAML, null),
                overrideProperty("+dev.timeout", "400", YAML, null),
            )
        assertEquals("400", timeoutOf(config, emptyList()))
    }

    @Test
    fun envOverrideWinsOverProfileSharedOne() {
        val config: RawConfig =
            configWithProfileOverrides(
                overrideProperty("+p1,p2.timeout", "600", YAML, null),
                overrideProperty("+dev,prod.timeout", "500", YAML, null),
            )
        assertEquals("500", timeoutOf(config, listOf("p1")))
    }

    @ParameterizedTest
    @CsvSource(value = ["p1, p2, 600", "p2, p1, 200"])
    fun sharedProfileOverrideActsAsOverrideOfEachListedProfile(
        first: String,
        second: String,
        expected: String,
    ) {
        val config: RawConfig =
            RawConfig(
                emptyList(),
                listOf(
                    property("timeout", "100", YAML, null),
                    overrideProperty("+p1.timeout", "200", YAML, null),
                    overrideProperty("+p1,p2.timeout", "600", YAML, null),
                ),
            )
        assertEquals(expected, timeoutOf(config, listOf(first, second)))
    }

    @Test
    fun rejectDuplicateSharedOverridesForSameEnv() {
        val config: RawConfig =
            configWithProfileOverrides(
                overrideProperty("+dev,prod.timeout", "500", YAML, null),
                overrideProperty("+dev,uat.timeout", "600", YAML, null),
            )
        assertThrows(IllegalStateException::class.java, { timeoutOf(config, emptyList()) })
    }

    private fun configWithProfileOverrides(vararg additionalProperties: Property): RawConfig {
        val properties =
            ArrayList<Property>(
                listOf(
                    property("timeout", "100", YAML, null),
                    overrideProperty("+p2.timeout", "300", YAML, null),
                    overrideProperty("+p1.timeout", "200", YAML, null),
                    overrideProperty("+inactive.timeout", "999", YAML, null),
                )
            )
        properties.addAll(additionalProperties.toList())
        return RawConfig(emptyList(), properties)
    }

    private fun timeoutOf(config: RawConfig, profiles: List<String>): String {
        return config
            .getBaseAndIncludedProperties({ include -> emptyMap() }, profiles, "dev")
            .getValue("timeout")
            .value
    }
}
