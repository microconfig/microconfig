package io.microconfig.templates;

import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.RootComponent;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(RootComponent currentComponent, File destinationDir,
                       Map<String, String> componentProperties, PropertyResolver propertyResolver);
}
