package io.microconfig.domain.impl.properties.repository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class IncludeDirective {
    private static final String PREFIX = "#include ";
    private static final String PREFIX2 = "#@include ";

    private final String line;

    public static boolean isInclude(String line) {
        String lower = line.toLowerCase();
        return lower.startsWith(PREFIX) || lower.startsWith(PREFIX2);
    }

    public static IncludeDirective from(String line) {
        return new IncludeDirective(line);
    }

    public List<Include> withDefaultEnv(String defaultEnv) {
        try {
            return parseIncludes(defaultEnv);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Can't parse include '" + line + "'. Supported format: #include component[optionalEnv], component2", e);
        }
    }

    private List<Include> parseIncludes(String defaultEnv) {
        String[] components = line.substring(componentStartIndex()).split(",");
        if (components.length == 0) {
            throw new IllegalArgumentException("Include must contain component names.");
        }

        return stream(components)
                .map(String::trim)
                .map(component -> Include.parse(component, defaultEnv))
                .collect(toList());
    }

    private int componentStartIndex() {
        String lower = line.toLowerCase();
        {
            int index = tryPrefix(lower, PREFIX);
            if (index >= 0) return index;
        }
        {
            int index = tryPrefix(lower, PREFIX2);
            if (index >= 0) return index;
        }

        throw new IllegalArgumentException("Include must start with " + PREFIX + " or " + PREFIX2);
    }

    private int tryPrefix(String value, String prefix) {
        int start = value.indexOf(prefix);
        return start >= 0 ? start + prefix.length() : -1;
    }
}
