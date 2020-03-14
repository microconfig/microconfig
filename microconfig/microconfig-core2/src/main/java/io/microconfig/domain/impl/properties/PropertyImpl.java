package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySource;
import io.microconfig.domain.Resolver;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import java.util.Collection;
import java.util.Map;

import static io.microconfig.io.OsUtil.isWindows;
import static io.microconfig.io.StreamUtils.toLinkedMap;
import static io.microconfig.io.StringUtils.unixLikePath;
import static java.util.stream.IntStream.range;
import static lombok.AccessLevel.PRIVATE;

@Getter
@EqualsAndHashCode
@RequiredArgsConstructor(access = PRIVATE)
public class PropertyImpl implements Property {
    private static final String TEMP_VALUE = "#var";

    private final String key;
    @With
    private final String value;
    @With
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

        return new PropertyImpl(key, value, envContext, temp, source);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, false, source);
    }

    public static Property tempProperty(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, true, source);
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
        return line.startsWith(TEMP_VALUE + " ");
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

    public static boolean containsYamlProperties(Collection<Property> properties) {
        return properties
                .stream()
                .map(Property::getSource)
                .filter(s -> s instanceof FilePropertySource)
                .map(FilePropertySource.class::cast)
                .anyMatch(FilePropertySource::isYaml);
    }

    public Property escapeOnWindows() {
        if (!isWindows()) return this;

        String escaped = ("user.home".equals(key)) ? unixLikePath(value) : escapeValue();
        return withValue(escaped);
    }

    String escapeValue() {
        String one = "\\";
        String two = "\\\\";
        return value.replace(two, one).replace(one, two);
    }

    @Override
    public String toString() {
        return (temp ? "#" : "") + key + ": " + value;
    }

    @Override
    public Property resolveBy(Resolver resolver) {
        return withValue(resolver.resolve(value));
    }
}