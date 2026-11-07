package io.microconfig.core.properties.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ComponentNotFoundExceptionTest {
    @Test
    fun test() {
        val exception = ComponentNotFoundException("p4")
        exception.withParentComponent("p3")
        exception.withParentComponent("p2")
        exception.withParentComponent("p1")
        try {
            throw exception
        } catch (e: ComponentNotFoundException) {
            assertEquals(
                "Component \'p4\' doesn\'t exist. Dependency chain: p1 -> p2 -> p3 -> p4",
                e.message,
            )
        }
    }
}
