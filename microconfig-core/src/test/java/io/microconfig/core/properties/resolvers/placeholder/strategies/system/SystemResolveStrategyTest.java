package io.microconfig.core.properties.resolvers.placeholder.strategies.system;

import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.properties.resolvers.placeholder.PlaceholderResolver;
import io.microconfig.utils.Os;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.core.properties.io.selector.ConfigIoFactory.configIo;
import static io.microconfig.core.properties.resolvers.placeholder.strategies.system.SystemResolveStrategy.SYSTEM_SOURCE;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SystemResolveStrategyTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "system | true  | C:\\temp\\new",
            "env    | true  | C:\\temp\\new",
            "system | false | C:\\temp\\new",
            "env    | false | C:\\temp\\new",
            "system | true  | \\\\server\\share\\folder",
            "env    | true  | \\\\server\\share\\folder",
    })
    void preservePathsWhenReadBackFromProperties(String component, boolean windows, String path) throws IOException {
        try (var os = mockStatic(Os.class)) {
            os.when(Os::isWindows).thenReturn(windows);

            String serialized = serializePath(component, path);

            assertEquals(path, readPath(serialized));
        }
    }

    @Test
    void keepWindowsUserHomeAsUnixPath() {
        try (var os = mockStatic(Os.class)) {
            os.when(Os::isWindows).thenReturn(true);
            var strategy = new SystemResolveStrategy(SYSTEM_SOURCE, key -> "C:\\Users\\test");

            String value = strategy.resolve(SYSTEM_SOURCE, "user.home", "dev", "app", null)
                    .orElseThrow().getValue();

            assertEquals("C:/Users/test", value);
        }
    }

    private String serializePath(String component, String path) {
        var strategy = new SystemResolveStrategy(component, key -> path);
        var resolver = new PlaceholderResolver(mock(EnvironmentRepository.class), strategy, emptySet());
        File file = new File("target/application.properties");
        var source = fileSource(file, 0, false, "app", "dev");
        String resolved = resolver.resolve("${" + component + "@path}", source, source);
        return configIo().writeTo(file).serialize(List.of(property("path", resolved, PROPERTIES, source)));
    }

    private String readPath(String serialized) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(serialized));
        return properties.getProperty("path");
    }
}
