package io.microconfig.configs.files.format;

import java.io.File;

public interface FileFormatDetector {
    FileFormat detectFormat(File file);
}