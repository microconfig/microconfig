package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.ConfigFormat;
import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static io.microconfig.utils.StringUtils.findFirstIndexIn;
import static lombok.AccessLevel.PRIVATE;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = PRIVATE)
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var ";

    private final String key;
    @With(PRIVATE)
    private final String value;
    private final boolean temp;
    private final ConfigFormat configFormat;

    private final DeclaringComponent declaringComponent;

    public static Property parse(String keyValue, ConfigFormat configFormat, DeclaringComponent source) {
        boolean temp = isTempProperty(keyValue);
        int separatorIndex = findSeparatorIndexIn(keyValue);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Property must contain ':' or '='. Bad property: " + keyValue + " in " + source);
        }

        String key = keyValue.substring(temp ? TEMP_VALUE.length() : 0, separatorIndex).trim();
        String value = keyValue.substring(separatorIndex + 1).trim();

        return new PropertyImpl(key, value, temp, configFormat, source);
    }

    public static Property property(String key, String value, ConfigFormat configFormat, DeclaringComponent source) {
        return new PropertyImpl(key, value, false, configFormat, source);
    }

    public static int findSeparatorIndexIn(String keyValue) {
        return findFirstIndexIn(keyValue, ":=");
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(TEMP_VALUE);
    }

    @Override
    public Property resolveBy(Resolver resolver, DeclaringComponent root) {
        try {
            String resolved = resolver.resolve(value, declaringComponent, root);
            return withValue(resolved);
        } catch (RuntimeException e) {
            throw new PropertyResolveException("Can't resolve property '" + this + "'", e); //todo
        }
    }

    @Override
    public String toString() {
        return (temp ? "#" : "") + key + "=" + value;
    }
}