package io.microconfig.commands.buildconfig.features.templates;

import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;

import java.io.File;
import java.util.Map;

public interface CopyTemplatesService {
    void copyTemplates(EnvComponent currentComponent,
                       File serviceDestinationDir,
                       Map<String, String> componentProperties,
                       PropertyResolver propertyResolver);
}
