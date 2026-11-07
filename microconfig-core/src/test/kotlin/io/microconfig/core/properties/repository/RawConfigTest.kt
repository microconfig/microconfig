package io.microconfig.core.properties.repository

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.OverrideProperty.Companion.overrideProperty
import io.microconfig.core.properties.Property
import io.microconfig.core.properties.PropertyImpl.Companion.literalProperty
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.ValueFormat
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RawConfigTest {
    @Test
    fun keepCachedBasePropertiesIndependentOfReturnedMapMutationsAndOverrides() {
        val base = property("value", "base", YAML, null)
        val config = RawConfig(emptyList(), listOf(base,
            overrideProperty("+prod.value", "production", YAML, null)))
        val first = config.getBaseAndIncludedProperties({ emptyMap() }, emptyList(), "dev")

        (first as MutableMap).clear()

        assertSame(base, config.getBaseAndIncludedProperties({ emptyMap() }, emptyList(), "dev")["value"])
        assertEquals("production", config.getBaseAndIncludedProperties({ emptyMap() }, emptyList(), "prod")["value"]!!.value)
        assertSame(base, config.getBaseAndIncludedProperties({ emptyMap() }, emptyList(), "dev")["value"])
    }

    @Test
    fun retainMixedKeyIdentitiesAndOriginalWinnersWhenMergingIncludes() {
        val yamlSource = fileSource(File("components/local/application.yaml"), 0, true, "app", "dev")
        val localPropertiesSource = fileSource(File("components/local/application.properties"), 1, false, "app", "dev")
        val propertiesSource = fileSource(File("components/included/application.properties"), 2, false, "app", "dev")
        val number = property("1", "number", YAML, yamlSource)
        val literalPath = property("\"a.b\"", "literal", YAML, yamlSource)
        val localPath = property("a\\u002eb", "local", PROPERTIES, localPropertiesSource)
        val stringNumber = property("\\u0031", "string", PROPERTIES, propertiesSource)
        val includedPath = property("a.b", "included", PROPERTIES, propertiesSource)
        val laterPath = property("\\u0061.b", "later", PROPERTIES, propertiesSource)
        val tail = literalProperty("tail", "first\r\nsecond", PROPERTIES, propertiesSource, ValueFormat.TEXT)
        val first = linkedMapOf(stringNumber.key to stringNumber, includedPath.key to includedPath,
            tail.key to property("tail", "old", PROPERTIES, propertiesSource))
        val second = linkedMapOf(laterPath.key to laterPath, tail.key to tail)
        val reads = mutableListOf<String>()
        val config = RawConfig(listOf(Include("first", "dev"), Include("second", "dev")),
            listOf(number, literalPath, localPath))

        val result = config.getBaseAndIncludedProperties({ include ->
            reads.add(include.component)
            if (include.component == "first") first else second
        }, emptyList(), "dev")

        assertEquals(listOf("first", "second"), reads)
        assertEquals(listOf("1", "\"a.b\"", "a\\u002eb", "\\u0031", "tail"), result.keys.toList())
        for (winner in listOf(number, literalPath, localPath, stringNumber, tail))
            assertSame(winner, result.getValue(winner.key))
        assertEquals(ValueFormat.TEXT, result.getValue("tail").valueFormat)
        assertTrue(result.getValue("tail").isLiteralText)
        assertEquals("first\r\nsecond", result.getValue("tail").value)
    }

    @Test
    fun preserveScopedOverridePriorityInsertionOrderAndWinningMetadata() {
        val yamlSource = fileSource(File("components/local/application.yaml"), 0, true, "app", "dev")
        val envSource = fileSource(File("components/local/application.dev.properties"), 3, false, "app", "dev")
        val first = property("first", "retained", YAML, yamlSource)
        val last = property("last", "retained", YAML, yamlSource)
        val profileWinner = overrideProperty("+p2.profileSetting", "second-profile", YAML, yamlSource)
        val specific = overrideProperty("+dev.setting", "specific", PROPERTIES, envSource,
            literalText = true, valueFormat = ValueFormat.TEXT)
        val config = RawConfig(listOf(Include("included", "dev")), listOf(
            first, property("setting", "base", YAML, yamlSource),
            property("profileSetting", "base", YAML, yamlSource), last,
            overrideProperty(".var.setting", "global", YAML, yamlSource),
            overrideProperty("+p1.setting", "first-profile", YAML, yamlSource),
            overrideProperty("+p1.profileSetting", "first-profile", YAML, yamlSource),
            profileWinner, specific,
            overrideProperty("+dev,prod.setting", "shared", YAML, yamlSource),
            overrideProperty("+p2.setting", "second-profile", YAML, yamlSource),
            overrideProperty("+dev.envOnly", "environment", YAML, yamlSource),
        ))
        val included = listOf(property("setting", "included", YAML, null),
            property("includedOnly", "included", YAML, null)).associateBy { it.key }

        val result = config.getBaseAndIncludedProperties({ included }, listOf("p1", "p2"), "dev")

        assertEquals(listOf("first", "setting", "profileSetting", "last", "envOnly", "includedOnly"),
            result.keys.toList())
        assertSame(first, result.getValue("first"))
        assertSame(last, result.getValue("last"))
        assertSame(profileWinner, result.getValue("profileSetting"))
        assertSame(specific, result.getValue("setting"))
        assertSame(envSource, result.getValue("setting").declaringComponent)
        assertEquals(PROPERTIES, result.getValue("setting").keyFormat)
        assertEquals(ValueFormat.TEXT, result.getValue("setting").valueFormat)
        assertTrue(result.getValue("setting").isLiteralText)
        assertFalse(result.getValue("setting").isVar)
    }

    @Test
    fun captureIncludedValuesBeforeTheNextResolverCallMutatesTheirBackingMap() {
        val original = property("value", "first", YAML, null)
        val backing = linkedMapOf(original.key to original)
        val config = RawConfig(listOf(Include("first", "dev"), Include("second", "dev")), emptyList())

        val result = config.getBaseAndIncludedProperties({ include ->
            if (include.component == "first") backing
            else {
                backing[original.key] = property(original.key, "changed", YAML, null)
                emptyMap()
            }
        }, emptyList(), "dev")

        assertSame(original, result.getValue(original.key))
    }

    @Test
    fun applyLogicalPropertiesKeyIdentityToInlineOverrides() {
        val config = RawConfig(emptyList(), listOf(
            property("a.b", "base", PROPERTIES, null),
            overrideProperty("+p1.a\\u002eb", "profile", PROPERTIES, null),
            overrideProperty("+dev.\\u0061.b", "environment", PROPERTIES, null),
        ))

        val properties = config.getBaseAndIncludedProperties({ emptyMap() }, listOf("p1"), "dev")

        assertEquals(mapOf("\\u0061.b" to "environment"), properties.mapValues { it.value.value })
    }

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
