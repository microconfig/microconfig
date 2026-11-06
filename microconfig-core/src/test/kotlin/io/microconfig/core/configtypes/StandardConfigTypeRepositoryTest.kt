package io.microconfig.core.configtypes

import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StandardConfigTypeRepositoryTest {
    @Test
    fun standardTypesRepo() {
        assertEquals(StandardConfigType.entries, StandardConfigTypeRepository().configTypes)
    }
}
