package io.microconfig.core.domain.impl.properties;

import io.microconfig.core.domain.Property;

import java.util.Collection;

import static java.util.stream.IntStream.range;

public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    public static Property parse(String keyValue, String envContext, PropertySource source) {
        boolean temp = isTempProperty(keyValue);
        int indexOfSeparator = separatorIndex(keyValue);
        if (indexOfSeparator < 0) {
            throw new IllegalArgumentException("Property must contain ':' or '='. Bad property: " + keyValue + " in " + source);
        }

        String key = keyValue.substring(temp ? TEMP_VALUE.length() + 1 : 0, indexOfSeparator).trim();
        String value = keyValue.substring(indexOfSeparator + 1).trim();
        // return new Property(key, value, envContext, temp, source);
        return null;
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
//        return new Property(key, value, envContext, false, source);
        return null;
    }

    public static boolean containsYamlProperties(Collection<Property> properties) {
//        return properties
//                .stream()
//                .map(Property::getSource)
//                .filter(s -> s instanceof FileSource)
//                .map(FileSource.class::cast)
//                .anyMatch(FileSource::isYaml);

        return true;//todo
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public boolean isTemp() {
        return false;
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(TEMP_VALUE + " ");
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
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
}