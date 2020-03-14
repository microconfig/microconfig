package io.microconfig.domain.impl.properties.parser;

import java.io.File;

public interface ComponentParser {
    ParsedComponent parse(File file, String environment);
}
