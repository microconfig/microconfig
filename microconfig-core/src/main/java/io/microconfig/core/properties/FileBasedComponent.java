package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class FileBasedComponent implements DeclaringComponent {
    private final File source;
    private final int lineNumber; //starts from 0
    private final boolean yaml;
    private final String configType;
    private final String environment;

    public static FileBasedComponent fileSource(File file, int lineNumber, boolean yaml,
                                                String configType, String environment) {
        return new FileBasedComponent(file, lineNumber, yaml, configType, environment);
    }

    @Override
    public String getComponent() {
        return source.getParentFile().getName();
    }

    @Override
    public String toString() {
        return source + ":" + (lineNumber + 1); //todo relative path
    }
}