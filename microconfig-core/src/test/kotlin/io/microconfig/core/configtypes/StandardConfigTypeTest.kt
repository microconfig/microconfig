package io.microconfig.core.configtypes

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StandardConfigTypeTest {
    @Test
    fun shouldImplementConfigType() {
        val app: ConfigType = APPLICATION
        assertEquals("app", app.typeName)
        assertEquals("app", APPLICATION.typeName)
        assertEquals("APPLICATION", APPLICATION.name)
        assertEquals(setOf(".yml", ".yaml", ".properties"), app.sourceExtensions)
        assertEquals("application", app.resultFileName)
    }
}
