package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.service.io.Io;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static io.microconfig.testutils.MicronconfigTestFactory.getEnvProvider;
import static io.microconfig.utils.CollectionUtils.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FileBasedEnvironmentProviderTest {
    private final EnvironmentProvider environmentProvider = getEnvProvider();

    @Test
    void testEnvDirExistCheck() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileBasedEnvironmentProvider(new File("badPath"), mock(EnvironmentParser.class), mock(Io.class)));
    }

    @Test
    void testEnvName() {
        Set<String> environmentNames = environmentProvider.getEnvironmentNames();
        assertEquals(setOf("test-component-exclude1", "p1", "placeholderAsDefaultValue", "test-component-exclude2",
                "aliases", "base-env", "var", "uat", "demo", "dev2", "dev", "test-env-include", "test-include-abstract-env",
                "duplicate-components", "e1", "e2", "e3", "e4", "baseYaml", "includeYaml"),
                environmentNames);
    }
}