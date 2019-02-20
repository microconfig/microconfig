package io.microconfig.commands.postprocessors;

import io.microconfig.commands.PropertiesPostProcessor;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.resolver.PropertyResolverHolder;
import io.microconfig.properties.resolver.RootComponent;
import io.microconfig.templates.CopyTemplatesService;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Map;

import static io.microconfig.properties.Property.asStringMap;

@RequiredArgsConstructor
public class CopyTemplatesPostProcessor implements PropertiesPostProcessor {
    private final CopyTemplatesService copyTemplatesService;

    @Override
    public void process(RootComponent currentComponent, File destinationDir, Map<String, Property> componentProperties, PropertiesProvider propertiesProvider) {
        if (propertiesProvider instanceof PropertyResolverHolder) {
            copyTemplatesService.copyTemplates(currentComponent, destinationDir, asStringMap(componentProperties), ((PropertyResolverHolder) propertiesProvider).getResolver());
        }
    }
}