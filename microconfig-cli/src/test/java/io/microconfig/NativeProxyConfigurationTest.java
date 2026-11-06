package io.microconfig;

import io.microconfig.core.Microconfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static io.microconfig.utils.IoUtils.readClasspathResource;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeProxyConfigurationTest {
    @TempDir
    Path root;

    @Test
    void registerCachedResolverProxyInNativeImage() throws IOException {
        createRepository();
        Object resolver = Microconfig.searchConfigsIn(root.toFile()).resolver();
        String configuration = readClasspathResource("META-INF/native-image/proxy-config.json").replaceAll("\\s+", "");
        String interfaces = interfacesOf(resolver);

        assertTrue(Proxy.isProxyClass(resolver.getClass()));
        assertTrue(configuration.contains(interfaces), "Missing native proxy registration for " + interfaces);
    }

    private void createRepository() throws IOException {
        Files.createDirectories(root.resolve("components"));
        Files.createDirectories(root.resolve("envs"));
        Files.writeString(root.resolve("envs/dev.yaml"), "portOffset: 1\n");
    }

    private String interfacesOf(Object proxy) {
        return Arrays.stream(proxy.getClass().getInterfaces())
                .map(type -> "\"" + type.getName() + "\"")
                .collect(joining(",", "[", "]"));
    }
}
