package io.microconfig.features.templates;

import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.RootComponent;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(RootComponent currentComponent, File destinationDir,
                       Map<String, String> componentProperties, PropertyResolver propertyResolver);
}
