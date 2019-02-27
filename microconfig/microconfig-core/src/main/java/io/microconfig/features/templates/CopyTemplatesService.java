package io.microconfig.features.templates;

import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.configs.resolver.EnvComponent;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(EnvComponent currentComponent, File destinationDir,
                       Map<String, String> componentProperties, PropertyResolver propertyResolver);
}
