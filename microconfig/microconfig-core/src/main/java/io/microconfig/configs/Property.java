package io.microconfig.configs;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.IntStream.range;

@Getter
@EqualsAndHashCode
public class Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    private final String value;
    private final String envContext;
    private final boolean temp;

    private final PropertySource source;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        boolean temp = isTempProperty(keyValue);
        int indexOfSeparator = separatorIndex(keyValue);
        if (indexOfSeparator < 0) {
            throw new IllegalArgumentException("Property must contain ':' or '='. Bad property: " + keyValue + " in " + source);
        }

        String key = keyValue.substring(temp ? TEMP_VALUE.length() + 1 : 0, indexOfSeparator).trim();
        String value = keyValue.substring(indexOfSeparator + 1).trim();

        return new Property(key, value, envContext, temp, source);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return new Property(key, value, envContext, false, source);
    }

    public static Property tempProperty(String key, String value, String envContext, PropertySource source) {
        return new Property(key, value, envContext, true, source);
    }

    private Property(String key, String value, String envContext, boolean temp, PropertySource source) {
        this.key = requireNonNull(key, "Property key is null");
        this.value = requireNonNull(value, "Property value is null");
        this.envContext = requireNonNull(envContext, "Property env context is null");
        this.temp = temp;
        this.source = requireNonNull(source, "Property source is null");
    }

    public static int separatorIndex(String keyValue) {
        return range(0, keyValue.length())
                .filter(i -> {
                    char c = keyValue.charAt(i);
                    return c == '=' || c == ':';
                })
                .findFirst()
                .orElse(-1);
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(TEMP_VALUE);
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static Map<String, String> withoutTempValues(Map<String, Property> properties) {
        return properties.entrySet()
                .stream()
                .filter(e -> !e.getValue().isTemp())
                .collect(toLinkedMap(Map.Entry::getKey, e -> e.getValue().getValue()));
    }

    public static Map<String, String> asStringMap(Map<String, Property> properties) {
        return properties.entrySet()
                .stream()
                .collect(toLinkedMap(Map.Entry::getKey, e -> e.getValue().getValue()));
    }

    public Property withNewValue(String value) {
        return new Property(key, value, envContext, temp, source);
    }

    @Override
    public String toString() {
        String keyValue = key + "=" + value;
        return temp ? ("#" + keyValue) : keyValue;
    }

}