package io.microconfig.properties.sources;

import io.microconfig.environments.Component;
import io.microconfig.properties.PropertySource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.environments.Component.bySourceFile;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class FileSource implements PropertySource {
    private final Component component;
    private final File source;
    private final int lineNumber; //starts from 0
    private final boolean yaml;

    public static PropertySource fileSource(File file, int lineNumber, boolean yaml) {
        return new FileSource(bySourceFile(file), file, lineNumber, yaml);
    }

    @Override
    public String toString() {
        return source.getAbsolutePath() + ":" + (lineNumber + 1);
    }
}