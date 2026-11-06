package io.microconfig.core.properties

import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FileBasedComponentTest {
    val file: File = File("root/components/comp/config.yaml")

    val source: FileBasedComponent = FileBasedComponent(file, 0, true, "app", "dev")

    @Test
    fun getters() {
        assertEquals(file, source.source)
        assertEquals(0, source.lineNumber)
        assertTrue(source.isYaml)
    }

    @Test
    fun factoryMethod() {
        assertEquals(source, fileSource(file, 0, true, "app", "dev"))
    }

    @Test
    fun parentComponent() {
        assertEquals("comp", source.component)
    }

    @Test
    fun string() {
        assertEquals("../components/comp/config.yaml:1", source.toString())
    }
}
