package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySource;
import io.microconfig.domain.StatementResolver;
import io.microconfig.utils.Os;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static io.microconfig.utils.StringUtils.unixLikePath;
import static java.util.stream.IntStream.range;
import static lombok.AccessLevel.PRIVATE;

@Getter
@EqualsAndHashCode
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
        int indexOfSeparator = separatorIndex(keyValue);
        if (indexOfSeparator < 0) {
            throw new IllegalArgumentException("Property must contain ':' or '='. Bad property: " + keyValue + " in " + source);
        }

        String key = keyValue.substring(temp ? TEMP_VALUE.length() : 0, indexOfSeparator).trim();
        String value = keyValue.substring(indexOfSeparator + 1).trim();

        return new PropertyImpl(key, value, envContext, temp, source);
    }

    public static Property property(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, false, source);
    }

    public static PropertyImpl tempProperty(String key, String value, String envContext, PropertySource source) {
        return new PropertyImpl(key, value, envContext, true, source);
    }

    public static int separatorIndex(String keyValue) {
        return range(0, keyValue.length())
                .filter(i -> {
                    char c = keyValue.charAt(i);
                    return c == '=' || c == ':';
                }).findFirst()
                .orElse(-1);
    }

    public static boolean isTempProperty(String line) {
        return line.startsWith(TEMP_VALUE);
    }

    public static boolean isComment(String line) {
        return line.startsWith("#");
    }

    public Property escapeOnWindows() {
        if (!Os.isWindows()) return this;

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
    public Property resolveBy(StatementResolver resolver) {
        return withValue(resolver.resolveRecursively(value));
    }
}