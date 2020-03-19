package io.microconfig.core.properties.impl.io.selector;

import io.microconfig.core.properties.impl.io.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}