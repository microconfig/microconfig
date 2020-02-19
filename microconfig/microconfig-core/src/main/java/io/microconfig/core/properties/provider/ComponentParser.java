package io.microconfig.core.properties.provider;

import java.io.File;

public interface ComponentParser {
    ParsedComponent parse(File file, String environment);
}
