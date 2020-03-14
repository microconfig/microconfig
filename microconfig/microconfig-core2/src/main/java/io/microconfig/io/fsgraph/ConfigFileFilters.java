package io.microconfig.io.fsgraph;

import java.io.File;
import java.util.Set;
import java.util.function.Predicate;

public class ConfigFileFilters {
    public static Predicate<File> defaultConfig(Set<String> extensions) {
        return fileEndsWith(extensions)
                .and(environmentCountIs(c -> c == 0));
    }

    public static Predicate<File> configForMultipleEnvironments(Set<String> extensions, String environment) {
        return fileEndsWith(extensions)
                .and(containsInName(environment))
                .and(environmentCountIs(c -> c > 1));
    }

    public static Predicate<File> configForOneEnvironment(Set<String> extensions, String environment) {
        return fileEndsWith(extensions)
                .and(containsInName(environment))
                .and(environmentCountIs(c -> c == 1));
    }

    private static Predicate<File> fileEndsWith(Set<String> extensions) {
        return file -> extensions.stream().anyMatch(ext -> file.getName().endsWith(ext));
    }

    private static Predicate<File> containsInName(String environment) {
        String envPart = '.' + environment + '.';
        return file -> file.getName().contains(envPart);
    }

    private static Predicate<File> environmentCountIs(Predicate<Long> envCountPredicate) {
        return file -> {
            long dotCount = file.getName()
                    .chars()
                    .filter(c -> c == '.')
                    .count();
            return envCountPredicate.test(dotCount - 1);
        };
    }
}