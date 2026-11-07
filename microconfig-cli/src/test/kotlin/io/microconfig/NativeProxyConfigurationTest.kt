package io.microconfig

import io.microconfig.core.Microconfig
import io.microconfig.utils.IoUtils.readClasspathResource
import java.lang.reflect.Proxy
import java.nio.file.Files
import java.nio.file.Path
import java.util.Arrays
import java.util.stream.Collectors.joining
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class NativeProxyConfigurationTest {
    @TempDir lateinit var root: Path

    @Test
    fun registerCachedResolverProxyInNativeImage() {
        createRepository()
        val resolver: Any = Microconfig.searchConfigsIn(root.toFile()).resolver()
        val configuration: String =
            readClasspathResource("META-INF/native-image/proxy-config.json")
                .replace(Regex("\\s+"), "")
        val interfaces: String = interfacesOf(resolver)
        assertTrue(Proxy.isProxyClass(resolver.javaClass))
        assertTrue(
            configuration.contains(interfaces),
            "Missing native proxy registration for " + interfaces,
        )
    }

    private fun createRepository() {
        Files.createDirectories(root.resolve("components"))
        Files.createDirectories(root.resolve("envs"))
        Files.writeString(root.resolve("envs/dev.yaml"), "{}\n")
    }

    private fun interfacesOf(proxy: Any): String {
        return Arrays.stream(proxy.javaClass.getInterfaces())
            .map({ type -> "\"" + type.name + "\"" })
            .collect(joining(",", "[", "]"))
    }
}
