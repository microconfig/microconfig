package io.microconfig.properties.files.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Predicate;

@Getter
@RequiredArgsConstructor
public class PropertyFilter implements Predicate<File> {
    private final String envPart;
    private final String fileExtension;

    public static PropertyFilter newDefaultComponentFilter(String fileExtension) {
        return new PropertyFilter(null, fileExtension);
    }

    public static PropertyFilter newEnvComponentFilter(String environment, String fileExtension) {
        return new PropertyFilter("." + environment + ".", fileExtension);
    }

    @Override
    public boolean test(File file) {
        String fileName = file.getName();
        if (!fileName.endsWith(fileExtension)) return false;

        return envPart == null ? defaultFile(fileName) : envFile(fileName);
    }

    private boolean defaultFile(String fileName) {
        return fileName.indexOf('.') == fileName.length() - fileExtension.length();
    }

    private boolean envFile(String fileName) {
        return fileName.contains(envPart);
    }
}