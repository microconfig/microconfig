package io.microconfig.io.formats.selector;

import io.microconfig.io.formats.ConfigFormat;

import java.io.File;

public interface ConfigFormatDetector {
    ConfigFormat detectConfigFormat(File file);
}