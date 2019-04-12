package io.microconfig.environments.filebased;

import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.utils.reader.FilesReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static io.microconfig.testutils.MicronconfigTestFactory.getEnvProvider;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FileBasedEnvironmentProviderTest {
    private final EnvironmentProvider environmentProvider = getEnvProvider();

    @Test
    void testEnvDirExistCheck() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileBasedEnvironmentProvider(new File("badPath"), mock(EnvironmentParserSelector.class), mock(FilesReader.class)));
    }

    @Test
    void testEnvName() {
        Set<String> environmentNames = environmentProvider.getEnvironmentNames();
        assertEquals(
                new HashSet<>(asList("test-component-exclude1", "p1", "test-component-exclude2",
                        "aliases", "base-env", "var", "uat", "demo", "dev2", "dev", "test-env-include", "test-include-abstract-env",
                        "duplicate-components", "e1", "e2", "e3", "e4", "baseYaml", "includeYaml")),
                environmentNames);
    }
}