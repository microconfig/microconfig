package io.microconfig.configs.properties;

import io.microconfig.configs.environment.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static deployment.util.StreamUtils.toLinkedMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public class Property {
    private final String key;
    private final String value;
    private final String envContext;

    private final Source source;
    private final boolean temp;

    public static Property parse(String keyValue, String envContext, Source source) {
        int indexOfSeparator = keyValue.indexOf('=');
        if (indexOfSeparator < 0) {
            throw new IllegalArgumentException("Can't split keyValue '" + keyValue + "' by =");
        }

        boolean temp = isTempProperty(keyValue);
        String key = keyValue.substring(temp ? 5 : 0, indexOfSeparator).trim();
        String value = keyValue.substring(indexOfSeparator + 1);
        return new Property(key, value, envContext, source, temp);
    }

    public Property(String key, String value, String envContext, Source source) {
        this(key, value, envContext, source, false);
    }

    public Property(String key, String value, String envContext, Source source, boolean temp) {
        this.key = requireNonNull(key, "Property key is null");
        this.value = requireNonNull(value, "Property value is null");
        this.envContext = requireNonNull(envContext, "Property env context is null");
        this.source = requireNonNull(source, "Property source is null");
        this.temp = temp;
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith("#var");
    }

    public static Map<String, String> withoutTempValues(Map<String, Property> properties) {
        return unmodifiableMap(properties.entrySet().stream()
                .filter(e -> !e.getValue().isTemp())
                .filter(e -> !e.getValue().getSource().isSystem())
                .collect(toLinkedMap(Map.Entry::getKey, e -> e.getValue().getValue())));
    }

    public Property withNewValue(String resolvedValue) {
        return new Property(key, resolvedValue, envContext, source, temp);
    }

    @Override
    public String toString() {
        return (source.isSystem() ? "#" : "") + key + "=" + value;
    }

    @Getter
    @EqualsAndHashCode
    @RequiredArgsConstructor
    public static class Source {
        public static final String SYSTEM = "SYSTEM";

        private final Component component;
        private final String sourceOfProperty;
        private final int line;

        public Source(Component component, String sourceOfProperty) {
            this(component, sourceOfProperty, -1);
        }

        public boolean isSystem() {
            return SYSTEM.equals(sourceOfProperty);
        }

        @Override
        public String toString() {
            return component.getType() + " -> " + sourceOfProperty;
        }
    }
}