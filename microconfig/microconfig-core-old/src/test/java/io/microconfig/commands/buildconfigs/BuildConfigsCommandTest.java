package io.microconfig.commands.buildconfigs;

import io.microconfig.commands.ComponentsToProcess;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.EnvironmentProvider;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.serializer.ConfigSerializer;
import io.microconfig.core.properties.sources.SpecialSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Map;

import static io.microconfig.core.environments.Component.byType;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildConfigsCommandTest {
    @Mock
    private EnvironmentProvider environmentProvider;
    @Mock
    private ConfigProvider configProvider;
    @Mock
    private ConfigSerializer configSerializer;
    @Mock
    private BuildConfigPostProcessor postProcessor;

    private BuildConfigsCommand command;

    @BeforeEach
    void setUp() {
        command = new BuildConfigsCommand(environmentProvider, configProvider, configSerializer, postProcessor);
    }

    @Test
    void testExecute() {
        String env = "env";
        ComponentsToProcess context = mock(ComponentsToProcess.class);
        Component c1 = byType("c1");
        when(context.components(environmentProvider)).thenReturn(singletonList(c1));
        when(context.env()).thenReturn(env);

        Map<String, Property> c1Properties = propertiesFor(c1);
        when(configProvider.getProperties(c1, env)).thenReturn(c1Properties);
        File output = new File("output", c1.getName());
        when(configSerializer.serialize(c1.getName(), env, c1Properties.values())).thenReturn(of(output));

        command.execute(context);
        verify(postProcessor).process(new EnvComponent(c1, env), c1Properties, configProvider, output);
    }

    private Map<String, Property> propertiesFor(Component c1) {
        return singletonMap("k", Property.property("k", "v", "env", new SpecialSource(c1, "file")));
    }
}