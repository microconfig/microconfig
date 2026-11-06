package io.microconfig.core.configtypes

import io.microconfig.core.configtypes.impl.ConfigTypeImpl.Companion.byName
import io.microconfig.core.configtypes.impl.ConfigTypeImpl.Companion.byNameAndExtensions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ConfigTypeImplTest {
    @Test
    fun createByNameAndExtension() {
        val app: ConfigType = byNameAndExtensions("app", setOf(".yaml"), "application")
        assertEquals("app", app.name)
        assertEquals("application", app.resultFileName)
        assertEquals(setOf(".yaml"), app.sourceExtensions)
    }

    @Test
    fun createByName() {
        val app: ConfigType = byName("app")
        assertEquals("app", app.name)
        assertEquals("app", app.resultFileName)
        assertEquals(setOf(".app"), app.sourceExtensions)
    }

    @Test
    fun extensionsShouldStartWithDot() {
        val withBadExtension: Set<String> = setOf(".json", "yaml")
        assertThrows(
            IllegalArgumentException::class.java,
            { byNameAndExtensions("app", withBadExtension, "application") },
        )
    }
}
