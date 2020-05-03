package io.microconfig.core.properties.io;

import io.microconfig.core.properties.Property;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.properties.PropertyImpl.isComment;
import static io.microconfig.utils.StreamUtils.toSortedMap;

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
        Map<Integer, String> result = new TreeMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!isComment(line)) continue;

            result.put(i, line);
        }
        return result;
    }
}