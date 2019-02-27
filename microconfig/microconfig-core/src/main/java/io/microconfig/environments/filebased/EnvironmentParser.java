package io.microconfig.environments.filebased;

import io.microconfig.environments.Environment;

import java.util.List;

public interface EnvironmentParser {
    Environment parse(String name, String content);

    List<String> supportedFormats();
}