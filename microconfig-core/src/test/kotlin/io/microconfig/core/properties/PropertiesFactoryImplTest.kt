package io.microconfig.core.properties

import io.microconfig.core.configtypes.StandardConfigType.APPLICATION
import io.microconfig.core.configtypes.StandardConfigType.PROCESS
import io.microconfig.core.properties.impl.PropertiesFactoryImpl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PropertiesFactoryImplTest {
    @Test
    fun shareRawReadsBetweenAliasesAndKeepEnvironmentAndTypeSeparate() {
        val reads = mutableListOf<String>()
        val factory = PropertiesFactoryImpl { component, environment, type ->
            reads.add("$component/$environment/${type.getName()}")
            mapOf("value" to PropertyImpl.property("value", environment, ConfigFormat.YAML, null))
        }

        val first = factory.getPropertiesOf("one", "original", "dev", listOf(APPLICATION))
        val second = factory.getPropertiesOf("two", "original", "dev", listOf(APPLICATION))
        factory.getPropertiesOf("one", "original", "prod", listOf(APPLICATION))
        factory.getPropertiesOf("one", "original", "dev", listOf(PROCESS))

        assertEquals("one", first.first().declaringComponent.component)
        assertEquals("two", second.first().declaringComponent.component)
        assertEquals(listOf("original/dev/app", "original/prod/app", "original/dev/process"), reads)
    }
}
