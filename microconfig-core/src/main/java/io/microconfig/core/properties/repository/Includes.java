package io.microconfig.core.properties.repository;

import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.forEach;
import static io.microconfig.utils.StringUtils.split;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class Includes {
    private static final String PREFIX = "#include ";

    private final String line;

    public static boolean isInclude(String line) {
        return line.startsWith(PREFIX);
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
        List<String> components = split(line.substring(PREFIX.length()), ",");
        if (components.isEmpty()) {
            throw new IllegalArgumentException("Include must contain component names.");
        }
        return forEach(components, component -> Include.parse(component, defaultEnv));
    }
}