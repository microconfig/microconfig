package io.microconfig.core.configtypes

import io.microconfig.core.configtypes.ConfigTypeFilters.configType
import io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithExtensionOf
import io.microconfig.core.configtypes.ConfigTypeFilters.configTypeWithName
import io.microconfig.core.configtypes.ConfigTypeFilters.eachConfigType
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ConfigTypeFiltersTest {
    val app: ConfigType = type("app")

    val process: ConfigType = type("process")

    val deploy: ConfigType = type("deploy")

    val supportedTypes: List<ConfigType> = listOf(app, process, deploy)

    @Test
    fun selectEachConfigType() {
        assertEquals(supportedTypes, eachConfigType().selectTypes(supportedTypes))
    }

    @Test
    fun useProvidedTypes() {
        val testType: ConfigType = type("provided")
        assertEquals(listOf(testType), configType(testType).selectTypes(supportedTypes))
    }

    @Test
    fun selectByConfigName() {
        assertEquals(
            listOf(app, deploy),
            configTypeWithName("app", "deploy").selectTypes(supportedTypes),
        )
    }

    @Test
    fun failOnUnsupportedName() {
        assertThrows(
            IllegalArgumentException::class.java,
            { configTypeWithName("app", "badName").selectTypes(supportedTypes) },
        )
    }

    @Test
    fun failOnEmptyNames() {
        assertThrows(
            IllegalArgumentException::class.java,
            { configTypeWithName().selectTypes(supportedTypes) },
        )
        assertThrows(
            IllegalArgumentException::class.java,
            { configType().selectTypes(supportedTypes) },
        )
    }

    @Test
    fun selectByFileExtension() {
        assertEquals(
            listOf(app),
            configTypeWithExtensionOf(File("application.app")).selectTypes(supportedTypes),
        )
    }

    @Test
    fun failOnFileWithoutExtension() {
        val badFile: File = File("application")
        assertThrows(
            IllegalArgumentException::class.java,
            { configTypeWithExtensionOf(badFile).selectTypes(supportedTypes) },
        )
    }

    private fun type(name: String): ConfigType {
        val ct: ConfigType = mock(ConfigType::class.java)
        `when`(ct.name).thenReturn(name)
        `when`(ct.resultFileName).thenReturn("file-" + name)
        `when`(ct.sourceExtensions).thenReturn(setOf("." + name))
        return ct
    }
}
