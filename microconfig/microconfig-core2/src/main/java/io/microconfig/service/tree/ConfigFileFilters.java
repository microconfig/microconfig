package io.microconfig.service.tree;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;

public class ConfigFileFilters {
    public static Predicate<File> defaultFilter(Set<String> fileExtensions) {
        return file -> endsWith(fileExtensions, file)
                && file.getName().indexOf('.') == file.getName().lastIndexOf('.');
    }

    public static Predicate<File> envSharedFilter(Set<String> fileExtensions, String environment) {
        return file -> endsWith(fileExtensions, file)
                && containsEnvPart(file, environment, false);
    }

    public static Predicate<File> envSpecificFilter(Set<String> fileExtensions, String environment) {
        return file -> endsWith(fileExtensions, file)
                && containsEnvPart(file, environment, true);
    }

    private static boolean endsWith(Set<String> fileExtensions, File file) {
        return fileExtensions
                .stream()
                .anyMatch(ext -> file.getName().endsWith(ext));
    }

    private static boolean containsEnvPart(File file, String environment, boolean singleEnv) {
        String fileName = file.getName();
        int indexOfEnv = fileName.indexOf('.' + environment + '.');
        if (indexOfEnv < 0) return false;

        long envCount = file.getName()
                .chars()
                .filter(c -> c == '.')
                .count() - 1;
        return singleEnv == (envCount == 1);
    }
}