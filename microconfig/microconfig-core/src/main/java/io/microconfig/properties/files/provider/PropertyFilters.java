package io.microconfig.properties.files.provider;

import java.io.File;
import java.util.function.Predicate;


public class PropertyFilters {
    public static Predicate<File> defaultComponentFilter(String fileExtension) {
        return file -> hasExtension(file, fileExtension)
                && file.getName().indexOf('.') == file.getName().lastIndexOf('.');
    }

    public static Predicate<File> envSharedFilter(String fileExtension, String environment) {
        return file -> hasExtension(file, fileExtension)
                && containsEnvPart(file, environment, false);
    }

    public static Predicate<File> envFilter(String fileExtension, String environment) {
        return file -> hasExtension(file, fileExtension)
                && containsEnvPart(file, environment, true);
    }

    private static boolean hasExtension(File file, String fileExtension) {
        return file.getName().endsWith(fileExtension);
    }

    private static boolean containsEnvPart(File file, String environment, boolean singleEnv) {
        String fileName = file.getName();
        int indexOfEnv = fileName.indexOf('.' + environment);
        if (indexOfEnv < 0) return false;

        boolean containsOneDot = fileName.lastIndexOf('.') == indexOfEnv + environment.length() + 1;
        return singleEnv == containsOneDot;
    }
}