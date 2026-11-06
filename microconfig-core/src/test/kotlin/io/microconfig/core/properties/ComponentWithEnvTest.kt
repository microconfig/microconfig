package io.microconfig.core.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComponentWithEnvTest {
    @Test
    fun testToString() {
        assertEquals("comp[env]", DeclaringComponentImpl("app", "comp", "env").toString())
    }
}
