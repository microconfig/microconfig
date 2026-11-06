package io.microconfig.core.properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.OverrideProperty.checkVarBlockPosition;
import static io.microconfig.core.properties.OverrideProperty.isEmptyBlock;
import static io.microconfig.core.properties.OverrideProperty.isOverrideProperty;
import static io.microconfig.core.properties.OverrideProperty.overrideProperty;
import static io.microconfig.core.properties.OverrideProperty.envVarProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverridePropertyTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // single env
            "+prod.key           | prod      | key",
            "+prod.a.b.c         | prod      | a.b.c",
            "+prod.@type         | prod      | @type",
            "+prod .key          | prod      | key",
            "+pr@d.key           | pr@d      | key",
            // env lists
            "+a,b.key            | a,b       | key",
            "+a, b.key           | a,b       | key",
            "+a ,b.key           | a,b       | key",
            "+a,b,c.key          | a,b,c     | key",
            "+a,b.@type          | a,b       | @type",
            "+a,a.key            | a         | key",
            "+a,pr@d.key         | a,pr@d    | key",
    })
    void parseEnvsAndKey(String key, String envs, String expectedKey) {
        OverrideProperty p = parse(key);

        assertEquals(asList(envs.split(",")), p.getEnvironments());
        assertEquals(expectedKey, p.getKey());
        assertFalse(p.isVar());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"+prod | prod", "+a, b | a,b", "+prod.datasource | prod"})
    void varLineInsideEnvBlockIsVarForBlockEnvs(String header, String envs) {
        OverrideProperty p = (OverrideProperty) envVarProperty(header, "#var key: v", YAML, null);

        assertEquals(asList(envs.split(",")), p.getEnvironments());
        assertEquals("key", p.getKey());
        assertEquals("v", p.getValue());
        assertTrue(p.isVar());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+a, +b", "+a,,b"})
    void rejectVarLineInsideMalformedEnvBlock(String header) {
        assertThrows(IllegalArgumentException.class, () -> envVarProperty(header, "#var key: v", YAML, null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+prod..var.oracle.host | prod | oracle.host",
            "+a, b..var.x           | a,b  | x",
    })
    void multilineVarInsideEnvBlockIsVarForBlockEnvs(String key, String envs, String expectedKey) {
        OverrideProperty p = parse(key);

        assertEquals(asList(envs.split(",")), p.getEnvironments());
        assertEquals(expectedKey, p.getKey());
        assertTrue(p.isVar());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"+prod..variable.x | .variable.x", "+prod.var.x | var.x"})
    void onlyDotVarIsVarBlockInsideEnvBlock(String key, String expectedKey) {
        OverrideProperty p = parse(key);

        assertEquals(expectedKey, p.getKey());
        assertFalse(p.isVar());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {".var.foo.bar | foo.bar", ".var.x | x"})
    void multilineVarIsForAllEnvs(String key, String expectedKey) {
        OverrideProperty p = parse(key);

        assertEquals(emptyList(), p.getEnvironments());
        assertEquals(expectedKey, p.getKey());
        assertTrue(p.isVar());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"+var.x | var", "+var, prod.x | var,prod"})
    void varIsAnOrdinaryEnvNameInEnvBlock(String key, String envs) {
        assertEquals(asList(envs.split(",")), parse(key).getEnvironments());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+prod.x | true", ".var.x | true", ".var | true",
            "@prod.x | false", ".@prod.x | false", "@var.x | false", ".@var.x | false",
            "var.x | false", ".variable.x | false", ".vars.x | false", "prod.x | false"
    })
    void recognizeOverrideKeys(String key, boolean isOverride) {
        assertEquals(isOverride, isOverrideProperty(key));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+prod | '' | true", "+a, b | '' | true", ".var | '' | true", "+prod..var | '' | true",
            "+prod | v | false", "+prod.x | '' | false", "+prod..var | v | false", "+prod..variable | '' | false"
    })
    void detectEmptyBlock(String key, String value, boolean isEmpty) {
        assertEquals(isEmpty, isEmptyBlock(key, value, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+dev, +test", "+dev,,test", "+dev, +test..var"})
    void rejectEmptyBlockWithMalformedEnvList(String key) {
        assertThrows(IllegalArgumentException.class, () -> isEmptyBlock(key, "", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".var.x", ".var", "+dev..var.x", "+dev..var", "+dev, test..var.x",
            "x", "a.var.b", "a.variable", "+dev.x", "+dev..variable.x", ".var.a.var"
    })
    void acceptVarBlockAtTopLevelOrDirectlyInsideEnvBlock(String key) {
        checkVarBlockPosition(key, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"datasource..var.x", "datasource..var", "+dev.datasource..var.x", ".var..var.x", "+dev..var..var.x"})
    void rejectVarBlockDeeperThanAllowed(String key) {
        assertThrows(IllegalArgumentException.class, () -> checkVarBlockPosition(key, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {".var", "+prod"})
    void rejectScalarInsteadOfBlock(String key) {
        assertThrows(IllegalArgumentException.class, () -> overrideProperty(key, "5", YAML, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+a,+b.key", "+a, +b.key", "++a.key"})
    void rejectRepeatedPrefix(String key) {
        assertThrows(IllegalArgumentException.class, () -> parse(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+a,,b.key", "+,a.key", "+a,.key", "+a,.b.key"})
    void rejectEmptyEnvInList(String key) {
        assertThrows(IllegalArgumentException.class, () -> parse(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+prod", "+a,b"})
    void rejectOverrideWithoutProperty(String key) {
        assertThrows(IllegalArgumentException.class, () -> parse(key));
    }

    private OverrideProperty parse(String key) {
        return (OverrideProperty) overrideProperty(key, "v", YAML, null);
    }
}
