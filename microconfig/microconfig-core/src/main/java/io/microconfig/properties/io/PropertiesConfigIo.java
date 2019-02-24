package io.microconfig.properties.io;

import io.microconfig.properties.Property;
import io.microconfig.utils.FileUtils;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.IoUtils.readAllLines;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

class PropertiesConfigIo implements ConfigIo {
    @Override
    public Map<String, String> read(File file) {
        if (!file.exists()) return emptyMap();

        Map<String, String> keyToValue = new LinkedHashMap<>();
        StringBuilder lastLine = new StringBuilder();
        for (String line : readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            lastLine.append(trimmed);
            if (isMultilineValue(trimmed)) {
                lastLine.append(LINES_SEPARATOR);
                continue;
            }

            int separatorIndex = separatorIndex(lastLine);
            if (separatorIndex < 0) {
                throw new IllegalArgumentException("Property must contain '=' or ':'. Bad property: " + trimmed + " in " + file);
            }
            String key = lastLine.substring(0, separatorIndex);
            String value = lastLine.substring(separatorIndex + 1);
            keyToValue.put(key, value);

            lastLine.setLength(0);
        }

        return keyToValue;
    }

    private boolean isMultilineValue(String line) {
        return line.endsWith("\\");
    }

    private int separatorIndex(StringBuilder line) {
        int eqIndex = line.indexOf("=");
        if (eqIndex < 0) return line.indexOf(":");

        int colonIndex = line.lastIndexOf(":", eqIndex - 1);
        return colonIndex < 0 ? eqIndex : colonIndex;
    }

    @Override
    public void write(File file, Map<String, String> properties) {
        doWrite(file, properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue()));
    }

    @Override
    public void write(File file, Collection<Property> properties) {
        doWrite(file, properties.stream()
                .filter(p -> !p.isTemp())
                .map(Property::toString));
    }

    @Override
    public void append(File file, Map<String, String> properties) {
        Stream<String> lines = properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue());

        doWrite(file, concat(of(LINES_SEPARATOR), lines), APPEND);
    }

    private void doWrite(File file, Stream<String> lines, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), lines.collect(joining(LINES_SEPARATOR)), openOptions);
    }
}