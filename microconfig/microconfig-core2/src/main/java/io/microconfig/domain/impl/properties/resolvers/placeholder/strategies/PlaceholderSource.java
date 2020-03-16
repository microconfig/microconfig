package io.microconfig.domain.impl.properties.resolvers.placeholder.strategies;

import io.microconfig.domain.PropertySource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor
public class PlaceholderSource implements PropertySource {
    public static final String ENV_SOURCE = "envDescriptor";
    public static final String ENV_OS_SOURCE = "env";
    public static final String SYSTEM_SOURCE = "system";
    public static final String COMPONENT_SOURCE = "component";

    private final String component;
    private final String value;

    public static PropertySource envSource(String component) {
        return new PlaceholderSource(component, ENV_SOURCE);
    }

    public static PropertySource templateSource(String component, File template) {
        return new PlaceholderSource(component, template.getAbsolutePath());
    }

    @Override
    public String getDeclaringComponentName() {
        return component;
    }

    @Override
    public String getDeclaringComponentType() {
        return component;
    }

    @Override
    public String toString() {
        return value;
    }
}