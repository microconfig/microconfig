package io.microconfig.core.properties.repository;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.repository.Include.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IncludeTest {
    @Test
    void testParse() {
        testInclude("comp", "env", parse("comp[env]", "dev"));
        testInclude("c.d_-q2", "dev", parse("c.d_-q2", "dev"));

        assertThrows(IllegalArgumentException.class, () -> parse("*", "e"));
    }

    private void testInclude(String expectedComp,
                             String expectedEnv,
                             Include actual) {
        assertEquals(expectedComp, actual.getComponent());
        assertEquals(expectedEnv, actual.getEnvironment());
    }

    @Test
    void testToString() {
        assertEquals("#include comp[dev]", parse("comp", "dev").toString());
    }
}