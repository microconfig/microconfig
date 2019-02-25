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
    private static final PropertySource SYSTEM_SOURCE = new PropertySource(Component.byType(""), "SYSTEM");

    private final Component component;
    private final String sourceOfProperty;
    private final int line;

    public static PropertySource systemSource() {
        return SYSTEM_SOURCE;
    }

    public PropertySource(Component component, String sourceOfProperty) {
        this(component, sourceOfProperty, -1);
    }

    public static PropertySource fileSource(File file, int lineNumber) {
        return new PropertySource(bySourceFile(file), file.getAbsolutePath(), lineNumber);
    }

    public boolean isSystem() {
        return this == SYSTEM_SOURCE;
    }

    @Override
    public String toString() {
        return sourceOfProperty + ":" + line;
    }
}
