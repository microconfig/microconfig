package io.microconfig.configs.io.ioservice;

import io.microconfig.configs.Property;
import io.microconfig.utils.reader.ConfigFileReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.configs.Property.isComment;
import static io.microconfig.utils.StreamUtils.toSortedMap;

@RequiredArgsConstructor
public abstract class AbstractConfigReader implements ConfigReader {
    protected final File file;
    protected final List<String> lines;

    protected AbstractConfigReader(File file, ConfigFileReader fileReader) {
        this(file, fileReader.readLines(file));
    }

    @Override
    public List<Property> properties(String env) {
        return properties(env, false);
    }

    @Override
    public Map<String, String> propertiesAsMap() {
        return propertiesToMap(false);
    }

    @Override
    public Map<String, String> escapeResolvedPropertiesAsMap() {
        return propertiesToMap(true);
    }

    private Map<String, String> propertiesToMap(boolean resolveEscape) {
        return properties("", resolveEscape)
                .stream()
                .collect(toSortedMap(Property::getKey, Property::getValue));
    }

    protected abstract List<Property> properties(String env, boolean resolveEscape);

    @Override
    public Map<Integer, String> commentsByLineNumber() {
        Map<Integer, String> result = new TreeMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (isComment(line)) {
                result.put(i, line);
            }
        }
        return result;
    }
}