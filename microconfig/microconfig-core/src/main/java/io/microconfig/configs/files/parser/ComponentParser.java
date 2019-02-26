package io.microconfig.configs.files.parser;

import java.io.File;

public interface ComponentParser {
    ParsedComponent parse(File file, String environment);
}
