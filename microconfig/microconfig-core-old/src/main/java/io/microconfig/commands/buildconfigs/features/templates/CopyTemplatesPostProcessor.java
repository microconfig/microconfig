package io.microconfig.commands.buildconfigs.features.templates;

import io.microconfig.commands.buildconfigs.BuildConfigPostProcessor;
import io.microconfig.core.properties.ConfigProvider;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.resolver.PropertyResolverHolder;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.core.properties.Property.asStringMap;

@RequiredArgsConstructor
public class CopyTemplatesPostProcessor implements BuildConfigPostProcessor {
    private final CopyTemplatesService copyTemplatesService;

    @Override
    public void process(EnvComponent currentComponent,
                        Map<String, Property> componentProperties,
                        ConfigProvider configProvider, File resultFile) {
        if (configProvider instanceof PropertyResolverHolder) {
            PropertyResolver resolver = ((PropertyResolverHolder) configProvider).getResolver();
            copyTemplatesService.copyTemplates(currentComponent, resultFile.getParentFile(), asStringMap(componentProperties), resolver);
        }
    }
}