package io.microconfig.domain.impl.properties.io.selector;

import io.microconfig.domain.impl.properties.io.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}