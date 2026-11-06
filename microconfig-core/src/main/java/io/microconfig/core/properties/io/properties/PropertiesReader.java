package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.AbstractConfigReader;
import io.microconfig.io.FsReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.PropertyImpl.findSeparatorIndexIn;
import static io.microconfig.core.properties.PropertyImpl.isDirective;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.util.stream.Collectors.toList;

class PropertiesReader extends AbstractConfigReader {
    private final List<LogicalLine> logicalLines;

    PropertiesReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
        logicalLines = readLogicalLines();
    }

    @Override
    public List<Property> properties(String configType, String environment) {
        return logicalLines.stream()
                .filter(line -> !line.directive())
                .map(line -> toProperty(line, configType, environment))
                .collect(toList());
    }

    @Override
    public Map<Integer, String> commentsByLineNumber() {
        return directiveLines();
    }

    private List<LogicalLine> readLogicalLines() {
        List<LogicalLine> result = new ArrayList<>();
        StringBuilder current = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (current == null) {
                if (line.isBlank()) continue;
                if (isDirective(line.stripLeading())) {
                    result.add(new LogicalLine(i, line.trim(), true));
                    continue;
                }
                current = new StringBuilder();
            } else if (line.isBlank()) {
                current.setLength(current.length() - LINES_SEPARATOR.length() - 1);
                result.add(new LogicalLine(i, current.toString(), false));
                current = null;
                continue;
            }

            boolean continued = isMultilineValue(line);
            String segment = line.stripLeading();
            boolean atEnd = i == lines.size() - 1;
            current.append(continued && atEnd ? segment.substring(0, segment.length() - 1) : segment);
            if (continued && !atEnd) {
                current.append(LINES_SEPARATOR);
            } else {
                result.add(new LogicalLine(i, current.toString(), false));
                current = null;
            }
        }
        return result;
    }

    private boolean isMultilineValue(String line) {
        int end = line.length();
        while (end > 0 && line.charAt(end - 1) == '\\') end--;
        return (line.length() - end) % 2 == 1;
    }

    private Property toProperty(LogicalLine line, String configType, String environment) {
        DeclaringComponent source = fileSource(file, line.lineNumber(), false, configType, environment);
        int separator = findSeparatorIndexIn(line.text());
        if (separator < 0) {
            throw new IllegalArgumentException("Incorrect delimiter in '" + line.text() + "' in '" + source
                    + "'\nProperty must contain ':' or '=' as delimiter.");
        }
        return property(line.text().substring(0, separator).trim(), line.text().substring(separator + 1).stripLeading(), PROPERTIES, source);
    }

    private Map<Integer, String> directiveLines() {
        Map<Integer, String> result = new TreeMap<>();
        logicalLines.stream()
                .filter(LogicalLine::directive)
                .forEach(line -> result.put(line.lineNumber(), line.text()));
        return result;
    }

    private record LogicalLine(int lineNumber, String text, boolean directive) {
    }
}
