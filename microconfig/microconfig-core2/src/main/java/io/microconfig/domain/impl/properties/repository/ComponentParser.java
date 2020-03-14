package io.microconfig.domain.impl.properties.repository;

import java.io.File;

public interface ComponentParser {
    ParsedComponent parse(File configFile, String environment);
}
