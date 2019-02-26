package io.microconfig.configs.files.format;

import java.io.File;

import static io.microconfig.configs.files.format.FileFormat.PROPERTIES;
import static io.microconfig.configs.files.format.FileFormat.YAML;

public class FileFormatDetectorImpl implements FileFormatDetector {
    @Override
    public FileFormat sourceFileFormat(File file) {
        return hasYamlExtension(file) ? YAML : PROPERTIES;
    }

    @Override
    public FileFormat outputFileFormat(File file) {
        return null;
    }

    private boolean hasYamlExtension(File file) {
        return file.getName().endsWith(YAML.extension());
    }
}