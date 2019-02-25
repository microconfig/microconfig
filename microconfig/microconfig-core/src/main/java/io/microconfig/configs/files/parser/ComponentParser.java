package io.microconfig.configs.files.parser;

import io.microconfig.environments.Component;

import java.io.File;

public interface ComponentParser {
    ParsedComponent parse(File file, Component component, String environment);
}
