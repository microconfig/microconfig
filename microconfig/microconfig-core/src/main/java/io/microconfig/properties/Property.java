package io.microconfig.properties;

import io.microconfig.environments.Component;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

@Getter
@EqualsAndHashCode
public class Property {
    private static final String VAR = "#var";

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
        String key = keyValue.substring(temp ? VAR.length() + 1 : 0, indexOfSeparator);
        String value = keyValue.substring(indexOfSeparator + 1);
        return new Property(key, value, envContext, source, temp);
    }

    public Property(String key, String value, String envContext, Source source) {
        this(key, value, envContext, source, false);
    }

    public Property(String key, String value, String envContext, Source source, boolean temp) {
        this.key = requireNonNull(key, "Property key is null").trim();
        this.value = requireNonNull(value, "Property value is null").trim();
        this.envContext = requireNonNull(envContext, "Property env context is null").trim();
        this.source = requireNonNull(source, "Property source is null");
        this.temp = temp;
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(VAR);
    }

    public static Map<String, String> withoutTempValues(Map<String, Property> properties) {
        return unmodifiableMap(properties.entrySet().stream()
                .filter(e -> !e.getValue().isTemp())
                .filter(e -> !e.getValue().getSource().isSystem())
                .collect(toLinkedMap(Map.Entry::getKey, e -> e.getValue().getValue())));
    }

    public static Map<String, String> asStringMap(Map<String, Property> properties) {
        return properties.entrySet().stream()
                .collect(toLinkedMap(Map.Entry::getKey, e -> e.getValue().getValue()));
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
        private static final Source SYSTEM_SOURCE = new Source(Component.byType(""), "SYSTEM");

        private final Component component;
        private final String sourceOfProperty;
        private final int line;

        public static Source systemSource() {
            return SYSTEM_SOURCE;
        }

        public Source(Component component, String sourceOfProperty) {
            this(component, sourceOfProperty, -1);
        }

        public boolean isSystem() {
            return this == SYSTEM_SOURCE;
        }

        @Override
        public String toString() {
            return component.getType() + " -> " + sourceOfProperty;
        }
    }
}