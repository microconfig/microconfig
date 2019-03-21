package io.microconfig.configs.sources;

import io.microconfig.configs.PropertySource;
import io.microconfig.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class SpecialSource implements PropertySource {
    public static final String ENV_DESCRIPTOR_SOURCE = "env_descriptor";
    public static final String ENV_OS_SOURCE = "env";
    public static final String SYSTEM_SOURCE = "system";

    private final Component component;
    private final String value;

    public static PropertySource envSource(Component component) {
        return new SpecialSource(component, ENV_DESCRIPTOR_SOURCE);
    }

    public static PropertySource templateSource(Component component, File template) {
        return new SpecialSource(component, template.getAbsolutePath());
    }

    @Override
    public String toString() {
        return value;
    }

    public boolean isSystem() {
        return SYSTEM_SOURCE.equals(value);
    }
}