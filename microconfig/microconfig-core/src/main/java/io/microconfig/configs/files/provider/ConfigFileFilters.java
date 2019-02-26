package io.microconfig.configs.files.provider;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;


public class ConfigFileFilters {
    public static Predicate<File> defaultComponentFilter(Set<String> fileExtensions) {
        return file -> hasExtension(file, fileExtensions)
                && file.getName().indexOf('.') == file.getName().lastIndexOf('.');
    }

    public static Predicate<File> envSharedFilter(Set<String> fileExtensions, String environment) {
        return file -> hasExtension(file, fileExtensions)
                && containsEnvPart(file, environment, false);
    }

    public static Predicate<File> envSpecificFilter(Set<String> fileExtensions, String environment) {
        return file -> hasExtension(file, fileExtensions)
                && containsEnvPart(file, environment, true);
    }

    public static Predicate<File> extensionFilter(Set<String> fileExtensions) {
        return file -> hasExtension(file, fileExtensions);
    }

    private static boolean hasExtension(File file, Set<String> fileExtensions) {
        return fileExtensions
                .stream()
                .anyMatch(ext -> file.getName().endsWith(ext));
    }

    private static boolean containsEnvPart(File file, String environment, boolean singleEnv) {
        String fileName = file.getName();
        int indexOfEnv = fileName.indexOf('.' + environment);
        if (indexOfEnv < 0) return false;

        boolean containsOneDot = fileName.lastIndexOf('.') == indexOfEnv + environment.length() + 1;
        return singleEnv == containsOneDot;
    }
}