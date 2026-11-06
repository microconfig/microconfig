package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ConfigWriter;
import io.microconfig.utils.FileUtils;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.nio.file.OpenOption;
import java.util.Collection;
import java.util.Map;

import static io.microconfig.core.properties.io.properties.PropertiesSyntax.endsWithContinuation;
import static io.microconfig.core.properties.io.properties.PropertiesSyntax.isSkippedBeforeValue;
import static io.microconfig.core.properties.io.properties.PropertiesSyntax.lineBreakEnd;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static java.util.stream.Collectors.joining;

/**
 * Values are kept the way they are written in .properties sources, with their escapes and continuations
 * (plain text inserted into them is escaped on insertion, see PropertiesText).
 * Only line breaks and leading whitespace that .properties would lose are escaped.
 */
@RequiredArgsConstructor
class PropertiesWriter implements ConfigWriter {
    private final File file;

    @Override
    public void write(Map<String, String> properties) {
        doWrite(serializeMap(properties));
    }

    @Override
    public void write(Collection<Property> properties) {
        doWrite(serialize(properties));
    }

    @Override
    public String serialize(Collection<Property> properties) {
        return properties.stream()
                .filter(p -> !p.isVar())
                .map(p -> p.getKey() + "=" + serializedValue(p))
                .collect(joining(LINES_SEPARATOR));
    }

    @Override
    public String serializedValue(Property property) {
        return escape(property.getValue());
    }

    private String serializeMap(Map<String, String> properties) {
        return properties.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + escape(e.getValue()))
                .collect(joining(LINES_SEPARATOR));
    }

    // a line break after an odd number of backslashes is a continuation of the source and stays as is
    private String escape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            int lineBreakEnd = lineBreakEnd(value, i);
            if (lineBreakEnd < 0) {
                result.append(value.charAt(i));
                continue;
            }
            result.append(endsWithContinuation(result) ? value.substring(i, lineBreakEnd + 1) : escapedLineBreak(value, i, lineBreakEnd));
            i = lineBreakEnd;
        }
        return escapeLeadingWhitespace(result.toString());
    }

    // a lone '\r' stays a carriage return, '\n' and '\r\n' are a line break
    private String escapedLineBreak(String value, int start, int end) {
        return start == end && value.charAt(start) == '\r' ? "\\r" : "\\n";
    }

    private String escapeLeadingWhitespace(String escaped) {
        return !escaped.isEmpty() && isSkippedBeforeValue(escaped.charAt(0)) ? "\\" + escaped : escaped;
    }

    private void doWrite(String lines, OpenOption... openOptions) {
        FileUtils.write(file.toPath(), lines, openOptions);
    }
}
