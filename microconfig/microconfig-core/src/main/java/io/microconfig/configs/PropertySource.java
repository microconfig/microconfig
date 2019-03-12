package io.microconfig.configs;

import io.microconfig.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static io.microconfig.environments.Component.bySourceFile;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class PropertySource {
    private final Component component;
    private final String sourceValue;
    private final int line;

    private final boolean yaml;

    public static PropertySource fileSource(File file, int lineNumber, boolean yaml) {
        return new PropertySource(bySourceFile(file), file.getAbsolutePath(), lineNumber, yaml);
    }

    public static PropertySource specialSource(Component component, String sourceOfProperty) {
        return new PropertySource(component, sourceOfProperty, -1, false);
    }

    @Override
    public String toString() {
        return line < 0 ? sourceValue : sourceValue + ":" + line;
    }
}