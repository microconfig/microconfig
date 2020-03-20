package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.PropertySource;
import io.microconfig.core.properties.Resolver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Collection;
import java.util.Map;

import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static io.microconfig.utils.StringUtils.findFirstIndexIn;
import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var ";

    private final String key;
    @With
    private final String value;
    @With
    private final String envContext;
    private final boolean temp;

    private final PropertySource source;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        boolean temp = isTempProperty(keyValue);
        int separatorIndex = findKeyValueSeparatorIndexIn(keyValue);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Property must contain ':' or '='. Bad property: " + keyValue + " in " + source);
        }

        String key = keyValue.substring(temp ? TEMP_VALUE.length() : 0, separatorIndex).trim();
        String value = keyValue.substring(separatorIndex + 1).trim();

        return new PropertyImpl(key, value, envContext, temp, source);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, false, source);
    }

    public static PropertyImpl tempProperty(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, true, source);
    }

    public static int findKeyValueSeparatorIndexIn(String keyValue) {
        return findFirstIndexIn(keyValue, ":=");
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(TEMP_VALUE);
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static Map<String, String> asKeyValue(Collection<Property> properties) {
        return properties.stream().collect(toLinkedMap(Property::getKey, Property::getValue));
    }

    @Override
    public Property resolveBy(Resolver resolver, String configType) {
        try {
            return withValue(resolver.resolve(value, envContext, configType));
        } catch (RuntimeException e) {
            throw new PropertyResolveException("Can't resolve property '" + this + "'", e); //todo
        }
    }

    @Override
    public String toString() {
        return (temp ? "#" : "") + key + ": " + value;
    }
}