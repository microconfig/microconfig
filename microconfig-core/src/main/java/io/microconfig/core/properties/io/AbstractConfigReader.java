package io.microconfig.core.properties.io;

import io.microconfig.core.properties.Property;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static io.microconfig.core.properties.PropertyImpl.isDirective;
import static io.microconfig.utils.StreamUtils.toSortedMap;
import static java.util.Collections.emptySet;

@RequiredArgsConstructor
public abstract class AbstractConfigReader implements ConfigReader {
    protected final File file;
    protected final List<String> lines;

    protected AbstractConfigReader(File file, FsReader fsReader) {
        this(file, fsReader.readLines(file));
    }

    @Override
    public Map<String, String> propertiesAsMap() {
        return properties("", "").stream()
                .collect(toSortedMap(Property::getKey, Property::getValue));
    }

    @Override
    public Map<Integer, String> commentsByLineNumber() {
        return directivesOutside(literalValueLines());
    }

    private Map<Integer, String> directivesOutside(Set<Integer> valueLines) {
        Map<Integer, String> result = new TreeMap<>();
        for (int i = 0; i < lines.size(); i++) {
            if (valueLines.contains(i)) continue;

            String line = lines.get(i).trim();
            if (!isDirective(line)) continue;

            result.put(i, line);
        }
        return result;
    }

    protected Set<Integer> literalValueLines() {
        return emptySet();
    }
}
