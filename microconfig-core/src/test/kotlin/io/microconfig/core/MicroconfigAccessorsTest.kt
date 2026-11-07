package io.microconfig.core

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class MicroconfigAccessorsTest {
    @Test
    fun exposeMicroconfigStateThroughLegacyMethodsWithoutDuplicateGetters() {
        val type = Microconfig::class.java
        val names = listOf(
            "rootDir", "destinationDir", "fsReader", "additionalPlaceholderResolvers",
            "additionalPropertiesRepositories", "dependencies",
        )

        for (name in names) {
            assertTrue(Modifier.isPublic(type.getMethod(name).modifiers), name)
            val getter = "get" + name.replaceFirstChar { it.uppercaseChar() }
            assertThrows(NoSuchMethodException::class.java, { type.getMethod(getter) }, getter)
        }
    }

    @Test
    fun exposeDependenciesThroughLegacyMethodsWithoutDuplicateGetters() {
        val type = Microconfig.Dependencies::class.java
        val names = listOf(
            "environments", "componentFactory", "configTypeRepository", "propertiesFactory",
            "componentGraph", "resolver",
        )

        for (name in names) {
            assertTrue(Modifier.isPublic(type.getMethod(name).modifiers), name)
            val getter = "get" + name.replaceFirstChar { it.uppercaseChar() }
            assertThrows(NoSuchMethodException::class.java, { type.getMethod(getter) }, getter)
        }
    }

    @Test
    fun retainConfigIoPropertyWithoutALegacyAccessor() {
        val getter = Microconfig.Dependencies::class.java.getMethod("getConfigIo")

        assertTrue(Modifier.isPublic(getter.modifiers))
    }
}
