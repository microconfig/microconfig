package io.microconfig.configs.files.format;

import java.io.File;

import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;

public class FileFormatDetectorImpl implements FileFormatDetector {
    @Override
    public FileFormat detectFileFormat(File file) {
        return hasYamlExtension(file) ? YAML : PROPERTIES;
    }

    private boolean hasYamlExtension(File file) {
        return file.getName().endsWith(YAML.extension());
    }
}