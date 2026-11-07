package io.microconfig.core.configtypes

import io.microconfig.core.ClasspathReader.Companion.classpathFile
import io.microconfig.core.configtypes.impl.CustomConfigTypeRepository.Companion.findDescriptorIn
import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository
import io.microconfig.io.impl.DumpedFsReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class CustomConfigTypeRepositoryTest {
    @Test
    fun loadCustomTypeWithDefaultExtension() {
        val customRepo: ConfigTypeRepository =
            findDescriptorIn(classpathFile("configTypes/defaultExtensions"), DumpedFsReader())
        val types: List<ConfigType> = customRepo.configTypes
        assertEquals(1, types.size)
        val type: ConfigType = types.get(0)
        assertEquals("custom", type.typeName)
        assertEquals("result", type.resultFileName)
        assertEquals(setOf(".custom"), type.sourceExtensions)
    }

    @Test
    fun loadCustomTypeWithoutAttributes() {
        val customRepo: ConfigTypeRepository =
            findDescriptorIn(classpathFile("configTypes/withoutAttributes"), DumpedFsReader())
        val types: List<ConfigType> = customRepo.configTypes
        assertEquals(1, types.size)
        val type: ConfigType = types.get(0)
        assertEquals("custom", type.typeName)
        assertEquals("custom", type.resultFileName)
        assertEquals(setOf(".custom"), type.sourceExtensions)
    }

    @ParameterizedTest
    @ValueSource(strings = ["configTypes/empty", "configTypes/withoutTypes"])
    fun descriptorWithoutTypesProvidesNoCustomTypes(dir: String) {
        val customRepo: ConfigTypeRepository =
            findDescriptorIn(classpathFile(dir), DumpedFsReader())
        assertEquals(emptyList<Any>(), customRepo.configTypes)
    }

    @Test
    fun loadCustomFilesFromFile() {
        val customRepo: ConfigTypeRepository =
            findDescriptorIn(classpathFile("configTypes"), DumpedFsReader())
        val expected: List<ConfigType> = StandardConfigTypeRepository().configTypes
        val actual: List<ConfigType> = customRepo.configTypes
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (expectedType, actualType) ->
            assertEquals(expectedType.typeName, actualType.typeName)
            assertEquals(expectedType.resultFileName, actualType.resultFileName)
            assertEquals(expectedType.sourceExtensions, actualType.sourceExtensions)
        }
    }
}
