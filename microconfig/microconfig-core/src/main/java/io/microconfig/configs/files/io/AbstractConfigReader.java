package io.microconfig.configs.files.io;

import io.microconfig.configs.Property;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.IoUtils.readAllLines;
import static io.microconfig.utils.StreamUtils.toSortedMap;

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
                .collect(toSortedMap(Map.Entry::getKey, p -> p.getValue().getValue()));
    }

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

    protected boolean isComment(String p) {
        return p.startsWith("#");
    }

    protected abstract Map<String, Property> parse(String env);
}
