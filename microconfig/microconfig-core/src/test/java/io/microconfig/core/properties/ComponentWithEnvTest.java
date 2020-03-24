package io.microconfig.core.properties;

import io.microconfig.core.properties.impl.DeclaringComponentImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentWithEnvTest {
    @Test
    void testToString() {
        assertEquals(
                "comp[env]",
                new DeclaringComponentImpl("app", "comp", "env").toString()
        );
    }
}