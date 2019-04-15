package io.microconfig.commands.buildconfig.features.templates;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.ResolvedConfigProvider;
import io.microconfig.environments.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.Map;

import static io.microconfig.configs.Property.asStringMap;
import static io.microconfig.configs.Property.property;
import static io.microconfig.configs.sources.FileSource.fileSource;
import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CopyTemplatesPostProcessorTest {
    @Mock
    private CopyTemplatesService copyTemplatesService;
    @Mock
    private ResolvedConfigProvider configProvider;
    @Mock
    private PropertyResolver propertyResolver;

    private CopyTemplatesPostProcessor copyTemplatesPostProcessor;

    @BeforeEach
    void setUp() {
        copyTemplatesPostProcessor = new CopyTemplatesPostProcessor(copyTemplatesService);
    }

    @Test
    void processInvoke() {
        Component c1 = Component.byType("c1");
        String env = "env";
        EnvComponent currentComponent = new EnvComponent(c1, env);
        File file = new File("c1/application.yaml");
        Map<String, Property> properties = singletonMap("key", property("key", "value", env, fileSource(file, 0, true)));

        when((configProvider).getResolver()).thenReturn(propertyResolver);
        copyTemplatesPostProcessor.process(currentComponent, properties, configProvider, file);
        verify(copyTemplatesService).copyTemplates(currentComponent, file.getParentFile(), asStringMap(properties), propertyResolver);

        copyTemplatesPostProcessor.process(currentComponent, properties, mock(ConfigProvider.class), file);
        verifyNoMoreInteractions(copyTemplatesService);
    }
}