package io.microconfig.configs.files.io.selector;

import java.io.File;

public interface ConfigFormatDetector {
    FileFormat detectConfigFormat(File file);
}