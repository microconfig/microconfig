package io.microconfig.configs.files.format;

import java.io.File;

public interface FileFormatDetector {
    FileFormat sourceFileFormat(File file);

    FileFormat outputFileFormat(File file);
}