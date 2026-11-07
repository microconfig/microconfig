package io.microconfig.core.properties.resolvers.placeholder.strategies.system

import io.microconfig.core.environments.EnvironmentRepository
import io.microconfig.core.properties.ConfigFormat.PROPERTIES
import io.microconfig.core.properties.FileBasedComponent.Companion.fileSource
import io.microconfig.core.properties.PropertyImpl.Companion.property
import io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo
import io.microconfig.core.properties.resolvers.placeholder.impl.PlaceholderResolver
import io.microconfig.core.properties.resolvers.placeholder.strategies.system.impl.SystemResolveStrategy
import io.microconfig.core.properties.resolvers.placeholder.strategies.system.impl.SystemResolveStrategy.Companion.SYSTEM_SOURCE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import java.io.File
import java.io.StringReader
import java.util.*

class SystemResolveStrategyTest {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value =
            [
                "system | true  | C:\\temp\\new",
                "env    | true  | C:\\temp\\new",
                "system | false | C:\\temp\\new",
                "env    | false | C:\\temp\\new",
                "system | true  | \\\\server\\share\\u000Colder",
                "env    | true  | \\\\server\\share\\u000Colder",
            ],
    )
    fun preservePathsWhenReadBackFromProperties(component: String, windows: Boolean, path: String) {
        val serialized = serializePath(component, path, windows)
        assertEquals(path, readPath(serialized))
    }

    @Test
    fun keepWindowsUserHomeAsUnixPath() {
        val strategy = SystemResolveStrategy(SYSTEM_SOURCE, { "C:\\Users\\test" }, windows = true)
        val value =
            strategy.resolve(SYSTEM_SOURCE, "user.home", "dev", "app", null).orElseThrow().value
        assertEquals("C:/Users/test", value)
    }

    private fun serializePath(component: String, path: String, windows: Boolean): String {
        val strategy = SystemResolveStrategy(component, { path }, windows)
        val resolver =
            PlaceholderResolver(mock(EnvironmentRepository::class.java), strategy, emptySet())
        val file: File = File("target/application.properties")
        val source = fileSource(file, 0, false, "app", "dev")
        val resolved: String = resolver.resolve("\${" + component + "@path}", source, source)
        return configIo()
            .writeTo(file)
            .serialize(listOf(property("path", resolved, PROPERTIES, source)))
    }

    private fun readPath(serialized: String): String {
        val properties: Properties = Properties()
        properties.load(StringReader(serialized))
        return properties.getProperty("path")
    }
}
