package io.microconfig.commands.buildconfigs.features.templates;

import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolver;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(EnvComponent currentComponent,
                       File serviceDestinationDir,
                       Map<String, String> componentProperties,
                       PropertyResolver propertyResolver);
}
