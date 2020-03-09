package io.microconfig.core.service.environments;

import io.microconfig.core.domain.Environment;

import java.io.File;

public interface EnvironmentParser {
    Environment parse(String name, File envFile);
}