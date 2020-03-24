package io.microconfig.core.properties.impl.io;

import io.microconfig.core.properties.Property;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.properties.impl.PropertyImpl.isComment;
import static io.microconfig.utils.StreamUtils.toSortedMap;

@RequiredArgsConstructor
public abstract class AbstractConfigReader implements ConfigReader {
    protected final File file;
    protected final List<String> lines;

    protected AbstractConfigReader(File file, FsReader fsReader) {
        this(file, fsReader.readLines(file));
    }

    @Override
    public List<Property> properties(String configType, String environment) {
        return properties(configType, environment, false);
    }

    protected abstract List<Property> properties(String configType, String environment, boolean resolveEscape);

    @Override
    public Map<String, String> propertiesAsMap() {
        return propertiesToMap(false);
    }

    @Override
    public Map<String, String> escapeResolvedPropertiesAsMap() {
        return propertiesToMap(true);
    }

    @Override
    public Map<Integer, String> commentsByLineNumber() {
        Map<Integer, String> result = new TreeMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isComment(line)) continue;

            result.put(i, line);
        }
        return result;
    }

    private Map<String, String> propertiesToMap(boolean resolveEscape) {
        return properties("", "", resolveEscape).stream()
                .collect(toSortedMap(Property::getKey, Property::getValue));
    }
}