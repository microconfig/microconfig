package io.microconfig.configs.files.format;

import java.io.File;

import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;

public class FileFormatDetectorImpl implements FileFormatDetector {
    @Override
    public FileFormat detectFormat(File file) {
        return file.getName().endsWith(".yaml") ? YAML : PROPERTIES;
    }
}
