package io.microconfig.core.environments.repository;

import io.microconfig.core.environments.ComponentFactory;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class YamlEnvironmentRepositoryTest {
    @TempDir
    Path root;

    FileEnvironmentRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(root.resolve("envs"));
        repository = new FileEnvironmentRepository(root.toFile(), new DumpedFsReader(),
                mock(ComponentFactory.class), mock(PropertiesFactory.class));
    }

    @Test
    void ignoresJsonWhenListingEnvironments() throws IOException {
        writeEnvironment("dev.yaml", "portOffset: 7\n");
        writeEnvironment("legacy.json", "invalid JSON and YAML: [");

        assertEquals(Set.of("dev"), repository.environmentNames());
        assertEquals(List.of("dev"), repository.environments().stream().map(Environment::getName).toList());
    }

    @Test
    void treatsJsonOnlyEnvironmentAsMissing() throws IOException {
        writeEnvironment("legacy.json", "{\"portOffset\": 99}");

        assertThrows(EnvironmentException.class, () -> repository.getByName("legacy"));
        Environment environment = repository.getOrCreateByName("legacy");
        assertEquals("legacy", environment.getName());
        assertNull(environment.getSource());
        assertEquals(0, environment.getPortOffset());
        assertTrue(environment.getAllComponents().asList().isEmpty());
    }

    @Test
    void selectsYamlWhenJsonHasTheSameName() throws IOException {
        Path yaml = writeEnvironment("dev.yaml", "portOffset: 7\n");
        writeEnvironment("dev.json", "{\"portOffset\": 99}");

        Environment environment = repository.getByName("dev");
        assertEquals(yaml.toFile(), environment.getSource());
        assertEquals(7, environment.getPortOffset());
        assertEquals(Set.of("dev"), repository.environmentNames());
    }

    @Test
    void cannotIncludeJsonEnvironment() throws IOException {
        writeEnvironment("dev.yaml", "include:\n  env: legacy\n");
        writeEnvironment("legacy.json", "{\"portOffset\": 99}");

        EnvironmentException exception = assertThrows(EnvironmentException.class,
                () -> repository.getByName("dev"));
        assertTrue(exception.getMessage().contains("Can't find env 'legacy'"));
    }

    private Path writeEnvironment(String name, String content) throws IOException {
        return Files.writeString(root.resolve("envs").resolve(name), content);
    }
}
