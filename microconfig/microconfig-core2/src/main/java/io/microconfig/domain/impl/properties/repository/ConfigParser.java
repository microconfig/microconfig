package io.microconfig.domain.impl.properties.repository;

import java.io.File;

public interface ConfigParser {
    ParsedConfig parse(File configFile, String environment);
}
