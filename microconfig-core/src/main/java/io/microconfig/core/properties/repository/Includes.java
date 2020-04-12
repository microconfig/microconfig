package io.microconfig.core.properties.repository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.forEach;
import static io.microconfig.utils.StringUtils.split;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class Includes {
    private static final String PREFIX = "#include ";
    private static final String PREFIX2 = "#@include ";

    private final String line;

    public static boolean isInclude(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith(PREFIX) || lower.startsWith(PREFIX2);
    }

    public static Includes from(String line) {
        return new Includes(line);
    }

    public List<Include> withDefaultEnv(String defaultEnv) {
        try {
            return parseIncludes(defaultEnv);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Can't parse include '" + line + "'. Supported format: #include component[optionalEnv], component2", e);
        }
    }

    private List<Include> parseIncludes(String defaultEnv) {
        List<String> components = split(line.substring(componentStartIndex()), ",");
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Include must contain component names.");
        }
        return forEach(components, component -> Include.parse(component, defaultEnv));
    }

    private int componentStartIndex() {
        String lower = line.toLowerCase();
        int index = tryPrefix(lower, PREFIX);
        if (index >= 0) return index;

        int index2 = tryPrefix(lower, PREFIX2);
        if (index2 >= 0) return index2;

        throw new IllegalArgumentException("Include must start with " + PREFIX + " or " + PREFIX2);
    }

    private int tryPrefix(String value, String prefix) {
        int start = value.indexOf(prefix);
        return start >= 0 ? start + prefix.length() : -1;
    }
}