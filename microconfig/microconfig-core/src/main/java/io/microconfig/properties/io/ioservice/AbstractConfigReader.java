package io.microconfig.properties.io.ioservice;

import io.microconfig.properties.Property;
import io.microconfig.utils.reader.FilesReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.properties.Property.isComment;
import static io.microconfig.utils.StreamUtils.toSortedMap;

@RequiredArgsConstructor
public abstract class AbstractConfigReader implements ConfigReader {
    protected final File file;
    protected final List<String> lines;

    protected AbstractConfigReader(File file, FilesReader fileReader) {
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
            if (!isComment(line)) continue;

            result.put(i, line);
        }
        return result;
    }
}