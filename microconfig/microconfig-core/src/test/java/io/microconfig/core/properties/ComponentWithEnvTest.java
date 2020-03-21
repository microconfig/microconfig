package io.microconfig.core.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentWithEnvTest {
    @Test
    void testToString(){
        assertEquals(
                "comp[env]",
                new ComponentWithEnv("app", "comp", "env").toString()
        );
    }
}