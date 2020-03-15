package io.microconfig.domain.impl.properties.repository;

import java.io.File;

public interface ConfigDefinitionParser {
    ConfigDefinition parse(File configFile, String environment);
}
