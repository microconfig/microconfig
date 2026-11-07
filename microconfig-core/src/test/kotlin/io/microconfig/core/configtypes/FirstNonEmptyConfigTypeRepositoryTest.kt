package io.microconfig.core.configtypes

import io.microconfig.core.configtypes.StandardConfigType.*
import io.microconfig.core.configtypes.impl.FirstNonEmptyConfigTypeRepository.Companion.composite
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class FirstNonEmptyConfigTypeRepositoryTest {
    val r1: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    val r2: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    val r3: ConfigTypeRepository = mock(ConfigTypeRepository::class.java)

    @Test
    fun returnsFirstNotEmptyTypes() {
        val r2Types: List<ConfigType> = listOf(APPLICATION, DEPLOY)
        val r3Types: List<ConfigType> = listOf(SECRET, PROCESS)
        `when`(r2.configTypes).thenReturn(r2Types)
        `when`(r3.configTypes).thenReturn(r3Types)
        assertSame(r2Types, composite(r1, r2, r3).configTypes)
        assertSame(r3Types, composite(r1, r3, r2).configTypes)
        assertSame(r2Types, composite(r2, r1).configTypes)
    }

    @Test
    fun testExceptionIfNoTypesConfigured() {
        assertThrows(IllegalStateException::class.java, { composite(r1).configTypes })
    }
}
