package io.microconfig.core.domain.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;


@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class FileSource implements PropertySource {
    private final File source;
    private final int lineNumber; //starts from 0
    private final boolean yaml;

    public static PropertySource fileSource(File file, int lineNumber, boolean yaml) {
//        return new FileSource(bySourceFile(file), file, lineNumber, yaml);
        return null;
    }

    @Override
    public String toString() {
        return source.getAbsolutePath() + ":" + (lineNumber + 1);
    }
}