package io.microconfig.configs.io.ioservice.selector;

import java.io.File;

public interface ConfigFormatDetector {
    FileFormat detectConfigFormat(File file);
}