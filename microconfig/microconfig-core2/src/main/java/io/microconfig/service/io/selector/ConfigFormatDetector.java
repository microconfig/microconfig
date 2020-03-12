package io.microconfig.service.io.selector;

import io.microconfig.service.io.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}