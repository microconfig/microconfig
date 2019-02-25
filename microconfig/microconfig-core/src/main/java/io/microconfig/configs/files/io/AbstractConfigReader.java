package io.microconfig.configs.files.io;

import io.microconfig.configs.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.IoUtils.readAllLines;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public abstract class AbstractConfigReader implements ConfigReader {
    protected final File file;
    protected final List<String> lines;

    protected AbstractConfigReader(File file) {
        this(file, readAllLines(file));
    }

    @Override
    public List<Property> properties(String env) {
        return new ArrayList<>(parse(env).values());
    }

    @Override
    public Map<String, String> propertiesAsMap() {
        return parse("")
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, p -> p.getValue().getValue()));
    }

    @Override
    public List<String> comments() {
        return lines.stream()
                .map(String::trim)
                .filter(this::isComment)
                .collect(toList());
    }

    protected boolean isComment(String p) {
        return p.startsWith("#");
    }

    protected abstract Map<String, Property> parse(String env);
}
