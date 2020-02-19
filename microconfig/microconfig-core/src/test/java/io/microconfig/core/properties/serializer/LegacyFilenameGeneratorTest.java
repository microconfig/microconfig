package io.microconfig.core.properties.serializer;

import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.properties.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyFilenameGeneratorTest {
    @Mock
    private FilenameGenerator delegate;
    @Mock
    private EnvironmentProvider environmentProvider;
    @Mock
    private List<Property> properties;

    private LegacyFilenameGenerator legacyFilenameGenerator;

    @BeforeEach
    void setUp() {
        legacyFilenameGenerator = new LegacyFilenameGenerator("application", delegate, environmentProvider);
    }

    @Test
    void fileFor() {
        String env1 = "env";
        String c1 = "c1";

        File original = new File("components/c1/.mgmt/application.properties");
        when(delegate.fileFor(c1, env1, properties)).thenReturn(original);

        assertEquals(original, legacyFilenameGenerator.fileFor(c1, env1, properties));
        Environment env = Mockito.mock(Environment.class);
        when(env.getSource()).thenReturn("env.json");
        when(environmentProvider.getByName(env1)).thenReturn(env);

        assertEquals(new File("components/c1/.mgmt/service.properties"), legacyFilenameGenerator.fileFor(c1, env1, properties));
    }
}