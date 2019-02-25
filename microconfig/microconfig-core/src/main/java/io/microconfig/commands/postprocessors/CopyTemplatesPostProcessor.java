package io.microconfig.commands.postprocessors;

import io.microconfig.commands.BuildConfigPostProcessor;
import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.PropertyResolverHolder;
import io.microconfig.configs.resolver.RootComponent;
import io.microconfig.templates.CopyTemplatesService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.configs.Property.asStringMap;

@RequiredArgsConstructor
public class CopyTemplatesPostProcessor implements BuildConfigPostProcessor {
    private final CopyTemplatesService copyTemplatesService;

    @Override
    public void process(RootComponent currentComponent, File destinationDir, Map<String, Property> componentProperties, ConfigProvider configProvider) {
        if (configProvider instanceof PropertyResolverHolder) {
            copyTemplatesService.copyTemplates(currentComponent, destinationDir, asStringMap(componentProperties), ((PropertyResolverHolder) configProvider).getResolver());
        }
    }
}