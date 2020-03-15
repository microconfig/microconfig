package io.microconfig.domain.impl.properties.repository;

import java.io.File;

public interface ConfigParser {
    ConfigDefinition parse(File configFile, String environment);
}
