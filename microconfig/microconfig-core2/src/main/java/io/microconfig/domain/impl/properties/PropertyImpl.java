package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.Resolver;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    @With(PRIVATE)
    private final String value;
    private final boolean temp;

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        return null;
    }

    public static Property property(String key, String value) {
        return new PropertyImpl(key, value, false);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return null;
    }

    public static boolean containsYamlProperties(List<Property> properties) {
        return true;
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public static boolean isTempProperty(String value) {
        return false;
    }

    @Override
    public Property resolveBy(Resolver resolver) {
        return withValue(resolver.resolve(value));
    }
}