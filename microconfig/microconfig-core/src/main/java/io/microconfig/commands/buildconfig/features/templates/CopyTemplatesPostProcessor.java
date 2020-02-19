package io.microconfig.commands.buildconfig.features.templates;

import io.microconfig.commands.buildconfig.BuildConfigPostProcessor;
import io.microconfig.properties.ConfigProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.EnvComponent;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.PropertyResolverHolder;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.properties.Property.asStringMap;

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