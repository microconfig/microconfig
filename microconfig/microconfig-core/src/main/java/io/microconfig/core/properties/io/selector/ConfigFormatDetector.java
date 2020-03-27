package io.microconfig.core.properties.io.selector;

import io.microconfig.core.properties.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}