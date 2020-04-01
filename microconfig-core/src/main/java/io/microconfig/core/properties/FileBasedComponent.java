package io.microconfig.core.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.utils.StringUtils.unixLikePath;

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
        return relativeSource() + ":" + (lineNumber + 1);
    }

    private String relativeSource() {
        String path = unixLikePath(source.toString());
        int rootIndex = path.indexOf("/components/");
        return rootIndex < 0 ? path : ".." + path.substring(rootIndex);
    }
}