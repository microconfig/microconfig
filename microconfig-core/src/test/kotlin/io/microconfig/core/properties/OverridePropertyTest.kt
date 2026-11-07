package io.microconfig.core.properties

import io.microconfig.core.properties.ConfigFormat.YAML
import io.microconfig.core.properties.OverrideProperty.Companion.checkMultilineVarPosition
import io.microconfig.core.properties.OverrideProperty.Companion.envVarProperty
import io.microconfig.core.properties.OverrideProperty.Companion.isEmptyBlock
import io.microconfig.core.properties.OverrideProperty.Companion.isOverrideProperty
import io.microconfig.core.properties.OverrideProperty.Companion.overrideProperty
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class OverridePropertyTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            [
                "+prod.key           | prod      | key",
                "+prod.a.b.c         | prod      | a.b.c",
                "+prod.@type         | prod      | @type",
                "+prod .key          | prod      | key",
                "+pr@d.key           | pr@d      | key",
                "+a,b.key            | a,b       | key",
                "+a, b.key           | a,b       | key",
                "+a ,b.key           | a,b       | key",
                "+a,b,c.key          | a,b,c     | key",
                "+a,b.@type          | a,b       | @type",
                "+a,a.key            | a         | key",
                "+a,pr@d.key         | a,pr@d    | key",
            ],
    )
    fun parseEnvsAndKey(key: String, envs: String, expectedKey: String) {
        val p: OverrideProperty = parse(key)
        assertEquals(envs.split(","), p.environments)
        assertEquals(expectedKey, p.key)
        assertFalse(p.isVar)
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = ["+prod | prod", "+a, b | a,b", "+prod.datasource | prod"])
    fun varLineInsideEnvBlockIsVarForBlockEnvs(header: String, envs: String) {
        val p: OverrideProperty =
            (envVarProperty(header, "#var key: v", YAML, null) as OverrideProperty)
        assertEquals(envs.split(","), p.environments)
        assertEquals("key", p.key)
        assertEquals("v", p.value)
        assertTrue(p.isVar)
    }

    @ParameterizedTest
    @ValueSource(strings = ["+a, +b", "+a,,b"])
    fun rejectVarLineInsideMalformedEnvBlock(header: String) {
        assertThrows(
            IllegalArgumentException::class.java,
            { envVarProperty(header, "#var key: v", YAML, null) },
        )
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            ["+prod..var.oracle.host | prod | oracle.host", "+a, b..var.x           | a,b  | x"],
    )
    fun multilineVarInsideEnvBlockIsVarForBlockEnvs(
        key: String,
        envs: String,
        expectedKey: String,
    ) {
        val p: OverrideProperty = parse(key)
        assertEquals(envs.split(","), p.environments)
        assertEquals(expectedKey, p.key)
        assertTrue(p.isVar)
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = ["+prod..variable.x | .variable.x", "+prod.var.x | var.x"])
    fun onlyDotVarIsMultilineVarInsideEnvBlock(key: String, expectedKey: String) {
        val p: OverrideProperty = parse(key)
        assertEquals(expectedKey, p.key)
        assertFalse(p.isVar)
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = [".var.foo.bar | foo.bar", ".var.x | x"])
    fun multilineVarIsForAllEnvs(key: String, expectedKey: String) {
        val p: OverrideProperty = parse(key)
        assertEquals(emptyList<Any>(), p.environments)
        assertEquals(expectedKey, p.key)
        assertTrue(p.isVar)
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = ["+var.x | var", "+var, prod.x | var,prod"])
    fun varIsAnOrdinaryEnvNameInEnvBlock(key: String, envs: String) {
        assertEquals(envs.split(","), parse(key).environments)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            [
                "+prod.x | true",
                ".var.x | true",
                ".var | true",
                "@prod.x | false",
                ".@prod.x | false",
                "@var.x | false",
                ".@var.x | false",
                "var.x | false",
                ".variable.x | false",
                ".vars.x | false",
                "prod.x | false",
            ],
    )
    fun recognizeOverrideKeys(key: String, isOverride: Boolean) {
        assertEquals(isOverride, isOverrideProperty(key))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            [
                "+prod | \'\' | true",
                "+a, b | \'\' | true",
                ".var | \'\' | true",
                "+prod..var | \'\' | true",
                "+prod | v | false",
                "+prod.x | \'\' | false",
                "+prod..var | v | false",
                "+prod..variable | \'\' | false",
            ],
    )
    fun detectEmptyBlock(key: String, value: String, isEmpty: Boolean) {
        assertEquals(isEmpty, isEmptyBlock(key, value, null))
    }

    @ParameterizedTest
    @ValueSource(strings = ["+dev, +test", "+dev,,test", "+dev, +test..var"])
    fun rejectEmptyBlockWithMalformedEnvList(key: String) {
        assertThrows(IllegalArgumentException::class.java, { isEmptyBlock(key, "", null) })
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                ".var.x",
                ".var",
                "+dev..var.x",
                "+dev..var",
                "+dev, test..var.x",
                "x",
                "a.var.b",
                "a.variable",
                "+dev.x",
                "+dev..variable.x",
                ".var.a.var",
            ]
    )
    fun acceptMultilineVarAtTopLevelOrDirectlyInsideEnvBlock(key: String) {
        checkMultilineVarPosition(key, null)
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "datasource..var.x",
                "datasource..var",
                "+dev.datasource..var.x",
                ".var..var.x",
                "+dev..var..var.x",
            ]
    )
    fun rejectMultilineVarDeeperThanAllowed(key: String) {
        assertThrows(IllegalArgumentException::class.java, { checkMultilineVarPosition(key, null) })
    }

    @ParameterizedTest
    @ValueSource(strings = [".var", "+prod", "+prod..var", "+a, b..var"])
    fun rejectScalarInsteadOfBlock(key: String) {
        assertThrows(
            IllegalArgumentException::class.java,
            { overrideProperty(key, "5", YAML, null) },
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["+a,+b.key", "+a, +b.key", "++a.key"])
    fun rejectRepeatedPrefix(key: String) {
        assertThrows(IllegalArgumentException::class.java, { parse(key) })
    }

    @ParameterizedTest
    @ValueSource(strings = ["+a,,b.key", "+,a.key", "+a,.key", "+a,.b.key"])
    fun rejectEmptyEnvInList(key: String) {
        assertThrows(IllegalArgumentException::class.java, { parse(key) })
    }

    @ParameterizedTest
    @ValueSource(strings = ["+prod", "+a,b"])
    fun rejectOverrideWithoutProperty(key: String) {
        assertThrows(IllegalArgumentException::class.java, { parse(key) })
    }

    private fun parse(key: String): OverrideProperty {
        return (overrideProperty(key, "v", YAML, null) as OverrideProperty)
    }
}
