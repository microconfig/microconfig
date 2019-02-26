package io.microconfig.configs.files.format;

import java.io.File;

public interface ConfigFormatDetector {
    FileFormat detectConfigFormat(File file);
}