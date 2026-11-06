package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.FileBasedComponent;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.AbstractConfigReader;
import io.microconfig.io.FsReader;
import lombok.RequiredArgsConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.scanner.Scanner;
import org.yaml.snakeyaml.scanner.ScannerImpl;
import org.yaml.snakeyaml.tokens.ScalarToken;
import org.yaml.snakeyaml.tokens.Token;

import java.io.CharArrayReader;
import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.ConfigFormat.YAML;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.OverrideProperty.checkMultilineVarPosition;
import static io.microconfig.core.properties.OverrideProperty.envVarProperty;
import static io.microconfig.core.properties.OverrideProperty.isEmptyBlock;
import static io.microconfig.core.properties.OverrideProperty.isEnvBlock;
import static io.microconfig.core.properties.OverrideProperty.isOverrideProperty;
import static io.microconfig.core.properties.OverrideProperty.overrideProperty;
import static io.microconfig.core.properties.PropertyImpl.isDirective;
import static io.microconfig.core.properties.PropertyImpl.isTempProperty;
import static io.microconfig.core.properties.PropertyImpl.property;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.isBlank;
import static java.lang.Character.isWhitespace;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.binarySearch;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.DOUBLE_QUOTED;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.SINGLE_QUOTED;
import static org.yaml.snakeyaml.tokens.Token.ID.StreamEnd;

class YamlReader extends AbstractConfigReader {
    private final char[] source;
    private final int[] lineOffsets;
    private final int[] codePointLineOffsets;
    private final Map<Integer, Integer> flowValueEnds = new LinkedHashMap<>();
    private final Map<Integer, BlockScalar> blockScalars;
    private final Set<Integer> literalValueLines;

    YamlReader(File file, FsReader fileFsReader) {
        super(file, fileFsReader);
        source = join("\n", lines).toCharArray();
        lineOffsets = lineOffsets();
        codePointLineOffsets = codePointLineOffsets();
        Set<Integer> quotedLines = findQuotedValueLines();
        blockScalars = readBlockScalars(quotedLines);
        literalValueLines = findLiteralValueLines(quotedLines);
    }

    private int[] lineOffsets() {
        int[] offsets = new int[lines.size()];
        for (int i = 1; i < lines.size(); i++) offsets[i] = offsets[i - 1] + lines.get(i - 1).length() + 1;
        return offsets;
    }

    private int[] codePointLineOffsets() {
        int[] offsets = new int[lines.size()];
        for (int i = 1; i < lines.size(); i++) {
            String previous = lines.get(i - 1);
            offsets[i] = offsets[i - 1] + previous.codePointCount(0, previous.length()) + 1;
        }
        return offsets;
    }

    @Override
    public List<Property> properties(String configType, String environment) {
        List<Property> properties = parseProperties(configType, environment);
        properties.addAll(envVarProperties(configType, environment));
        return properties;
    }

    // '#var' lines inside '+env:' blocks are temp properties of the block envs, see envVarProperties
    @Override
    public Map<Integer, String> commentsByLineNumber() {
        Map<Integer, String> comments = super.commentsByLineNumber();
        envBlockHeaderByVarLine().keySet().forEach(comments::remove);
        return comments;
    }

    @Override
    protected Set<Integer> literalValueLines() {
        return literalValueLines;
    }

    private Set<Integer> findQuotedValueLines() {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            if (skip(lines.get(i))) continue;
            BlockScalar scalar = readBlockScalar(i);
            if (scalar != null) {
                i = scalar.endLine();
                continue;
            }
            int offset = quotedValueStart(lines.get(i));
            if (offset >= 0) i = Math.max(i, collectQuotedValueLines(i, offset, result));
        }
        return result;
    }

    private int quotedValueStart(String line) {
        int offset = offsetIndex(line);
        char first = line.charAt(offset);
        if (sequenceValueStart(line, offset) != offset || first == '[' || first == '{') return 0;

        int separator = mappingSeparatorIndex(line, offset);
        int value = skipNodeProperties(line, separator < 0 ? offset : skipWhitespace(line, separator + 1));
        if (value == line.length()) return -1;
        char marker = line.charAt(value);
        return marker == '\'' || marker == '"' || marker == '[' || marker == '{' ? value : -1;
    }

    private int collectQuotedValueLines(int startLine, int offset, Set<Integer> valueLines) {
        int start = lineOffsets[startLine] + offset;
        int codePointStart = codePointLineOffsets[startLine] + lines.get(startLine).codePointCount(0, offset);
        int lastLine = startLine;
        int depth = 0;
        boolean flow = false;
        try {
            Scanner scanner = new ScannerImpl(new StreamReader(new CharArrayReader(source, start, source.length - start)), new LoaderOptions());
            while (!scanner.checkToken(StreamEnd)) {
                Token token = scanner.getToken();
                switch (token.getTokenId()) {
                    case FlowSequenceStart, FlowMappingStart, BlockSequenceStart, BlockMappingStart -> {
                        if (depth == 0) flow = token.getTokenId() == Token.ID.FlowSequenceStart || token.getTokenId() == Token.ID.FlowMappingStart;
                        depth++;
                    }
                    case FlowSequenceEnd, FlowMappingEnd, BlockEnd -> {
                        if (--depth == 0) {
                            if (flow) {
                                int endLine = sourceLine(codePointStart + token.getEndMark().getIndex() - 1);
                                flowValueEnds.put(startLine, endLine);
                                return endLine;
                            }
                            return lastLine;
                        }
                        continue;
                    }
                    default -> { }
                }
                lastLine = Math.max(lastLine, sourceLine(codePointStart + token.getEndMark().getIndex() - 1));
                if (token instanceof ScalarToken scalar) {
                    if (scalar.getStyle() == DOUBLE_QUOTED || scalar.getStyle() == SINGLE_QUOTED) {
                        int firstLine = sourceLine(codePointStart + scalar.getStartMark().getIndex());
                        rangeClosed(firstLine + 1, lastLine).forEach(valueLines::add);
                    }
                    if (depth == 0) return lastLine;
                }
            }
        } catch (YAMLException ignored) {
            // Unresolved Microconfig expressions may prevent parsing the rest of the collection.
        }
        return lastLine;
    }

    private int sourceLine(int codePointIndex) {
        int line = binarySearch(codePointLineOffsets, codePointIndex);
        return line >= 0 ? line : -line - 2;
    }

    private Map<Integer, BlockScalar> readBlockScalars(Set<Integer> quotedLines) {
        Map<Integer, BlockScalar> result = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            if (quotedLines.contains(i)) continue;
            BlockScalar scalar = readBlockScalar(i);
            if (scalar == null) continue;
            result.put(i, scalar);
            i = scalar.endLine();
        }
        return result;
    }

    private BlockScalar readBlockScalar(int index) {
        String line = lines.get(index);
        if (skip(line)) return null;

        int offset = offsetIndex(line);
        int keyOffset = sequenceValueStart(line, offset);
        if (keyOffset == line.length()) return null;
        boolean sequence = keyOffset != offset;
        int nodeOffset = skipNodeProperties(line, keyOffset);
        if (nodeOffset == line.length()) return null;
        char first = line.charAt(nodeOffset);
        if (first == '[' || first == '{') return null;
        boolean valueOnly = sequence && (first == '|' || first == '>');
        int separator = valueOnly ? -1 : mappingSeparatorIndex(line, nodeOffset);
        if (separator < 0 && !valueOnly) return null;

        int valueOffset = separator < 0 ? keyOffset : skipWhitespace(line, separator + 1);
        int marker = skipNodeProperties(line, valueOffset);
        int indentation = scalarIndentation(line, marker);
        if (indentation < 0) return null;

        int baseOffset = separator < 0 ? line.lastIndexOf('-', keyOffset - 1) : keyOffset;
        int contentOffset = blockScalarContentOffset(index, baseOffset, indentation);
        int endLine = multiLineValueEnd(index, contentOffset);
        String key = separator < 0 ? null : line.substring(offset, separator).trim();
        return new BlockScalar(key, sequence, index, contentOffset, endLine, line.charAt(marker), chompingIndicator(line, marker), line.substring(valueOffset, marker).trim());
    }

    private int sequenceValueStart(String line, int offset) {
        int valueOffset = offset;
        while (valueOffset + 1 < line.length() && line.charAt(valueOffset) == '-' && isWhitespace(line.charAt(valueOffset + 1))) {
            valueOffset = skipWhitespace(line, valueOffset + 1);
        }
        return valueOffset;
    }

    private int skipWhitespace(String line, int offset) {
        while (offset < line.length() && isWhitespace(line.charAt(offset))) offset++;
        return offset;
    }

    private int mappingSeparatorIndex(String line, int keyOffset) {
        char quote = line.charAt(keyOffset);
        if (quote == '\'' || quote == '"') {
            int keyEnd = quotedKeyEnd(line, keyOffset, quote);
            if (keyEnd < 0) return -1;
            int separator = skipWhitespace(line, keyEnd + 1);
            return separator < line.length() && line.charAt(separator) == ':' ? separator : -1;
        }

        return separatorIndex(line, keyOffset);
    }

    private int quotedKeyEnd(String line, int offset, char quote) {
        for (int i = offset + 1; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote == '"' && c == '\\') {
                i++;
            } else if (c == quote) {
                if (quote == '\'' && i + 1 < line.length() && line.charAt(i + 1) == quote) i++;
                else return i;
            }
        }
        return -1;
    }

    private int skipNodeProperties(String line, int offset) {
        while (offset < line.length() && (line.charAt(offset) == '&' || line.charAt(offset) == '!')) {
            while (offset < line.length() && !isWhitespace(line.charAt(offset))) offset++;
            offset = skipWhitespace(line, offset);
        }
        return offset;
    }

    private int scalarIndentation(String line, int marker) {
        if (marker == line.length() || (line.charAt(marker) != '|' && line.charAt(marker) != '>')) return -1;
        int indentation = 0;
        boolean chomping = false;
        int i = marker + 1;
        for (; i < line.length() && !isWhitespace(line.charAt(i)); i++) {
            char c = line.charAt(i);
            if (c >= '1' && c <= '9' && indentation == 0) indentation = c - '0';
            else if ((c == '+' || c == '-') && !chomping) chomping = true;
            else return -1;
        }
        int tail = skipWhitespace(line, i);
        return tail == line.length() || line.charAt(tail) == '#' ? indentation : -1;
    }

    // '-' strips the final line break, '+' keeps the trailing ones, none keeps one
    private String chompingIndicator(String line, int marker) {
        for (int i = marker + 1; i < line.length() && !isWhitespace(line.charAt(i)); i++) {
            char c = line.charAt(i);
            if (c == '+' || c == '-') return String.valueOf(c);
        }
        return "";
    }

    private int blockScalarContentOffset(int index, int baseOffset, int indentation) {
        if (indentation > 0) return baseOffset + indentation;
        for (int i = index + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isBlank(line)) return Math.max(baseOffset + 1, offsetIndex(line));
        }
        return baseOffset + 1;
    }

    private int multiLineValueEnd(int index, int contentOffset) {
        for (int i = index + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isBlank(line) && offsetIndex(line) < contentOffset) return i - 1;
        }
        return lines.size() - 1;
    }

    private Set<Integer> findLiteralValueLines(Set<Integer> quotedLines) {
        Set<Integer> valueLines = new HashSet<>(quotedLines);
        blockScalars.values().forEach(scalar -> literalContentLines(scalar.lineNumber() + 1, scalar.endLine()).forEach(valueLines::add));
        return valueLines;
    }

    // lines of a literal value: blank lines between text lines are kept, leading and trailing ones are not
    private IntStream literalContentLines(int from, int to) {
        int first = from;
        while (first <= to && isBlank(lines.get(first))) first++;
        int last = to;
        while (last >= first && isBlank(lines.get(last))) last--;
        return rangeClosed(first, last);
    }

    private List<Property> parseProperties(String configType, String environment) {
        List<Property> result = new ArrayList<>();
        Deque<KeyOffset> currentProperty = new ArrayDeque<>();

        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (skip(line)) continue;

            int currentOffset = offsetIndex(line);
            BlockScalar scalar = blockScalars.get(lineNumber);
            if (scalar != null && !scalar.sequence()) {
                lineNumber = blockScalarValue(result, currentProperty, scalar, currentOffset, configType, environment);
            } else if (isListValue(line)) {
                lineNumber = listValue(result, currentProperty, currentOffset, lineNumber, configType, environment);
            } else if (isComplexValue(line, currentOffset)) {
                lineNumber = addComplexValue(result, currentProperty, currentOffset, lineNumber, configType, environment);
            } else {
                parseSimpleProperty(result, currentProperty, currentOffset, lineNumber, configType, environment);
            }
        }

        return result;
    }

    private boolean isListValue(String line) {
        String trim = line.trim();
        return !trim.isEmpty() && trim.charAt(0) == '-' && !trim.endsWith(":");
    }

    private int listValue(List<Property> result,
                          Deque<KeyOffset> currentProperty, int currentOffset,
                          int originalLineNumber, String configType, String env) {
        StringBuilder value = new StringBuilder();
        int index = originalLineNumber;
        while (true) {
            String line = lines.get(index);
            boolean includeLine = !skip(line) || literalValueLines.contains(index);
            if (includeLine) value.append(isBlank(line) ? "" : line.substring(currentOffset));

            if (index + 1 >= lines.size()) break;
            if (listValueEnd(lines.get(index + 1), currentOffset)) break;
            if (includeLine) value.append(LINES_SEPARATOR);

            ++index;
        }

        addValue(result, currentProperty, currentOffset, originalLineNumber, null, value.toString().trim(), configType, env);
        return index;
    }

    private boolean listValueEnd(String nextLine, int currentOffset) {
        if (skip(nextLine)) return false;

        int nextOffset = offsetIndex(nextLine);
        if (currentOffset > nextOffset) return true;
        return currentOffset == nextOffset && !isComplexValue(nextLine, currentOffset);
    }

    private int blockScalarValue(List<Property> result, Deque<KeyOffset> currentProperty, BlockScalar scalar, int offset, String configType, String env) {
        removePropertiesWithBiggerOffset(currentProperty, offset);
        FileBasedComponent source = fileSource(file, scalar.lineNumber(), true, configType, env);
        if (!scalar.nodeProperties().isEmpty()) {
            throw new IllegalArgumentException("YAML anchors and tags on standalone scalar properties are not supported in " + source);
        }
        List<String> valueLines = literalContentLines(scalar.lineNumber() + 1, scalar.endLine())
                .mapToObj(lines::get)
                .map(line -> isBlank(line) ? "" : line.substring(scalar.contentOffset()))
                .collect(toList());
        if (valueLines.isEmpty()) {
            throw new IllegalArgumentException("Missing value in multiline key '" + scalar.key() + "' in '"
                    + new FileBasedComponent(file, scalar.lineNumber(), true, configType, env) + "'");
        }
        String k = mergeKey(currentProperty, scalar.key());
        String v = scalar.style() == '>' ? FoldedScalar.toYamlValue(scalar.chomping(), valueLines) : join(LINES_SEPARATOR, valueLines);
        addProperty(result, k, v, source);
        return scalar.endLine();
    }

    private boolean isComplexValue(String line, int currentOffset) {
        char c = line.charAt(currentOffset);
        return asList('-', '[', ']', '{').contains(c) ||
                (c == '$' && line.length() > currentOffset + 1 && line.charAt(currentOffset + 1) == '{');
    }

    private int addComplexValue(List<Property> result,
                                Deque<KeyOffset> currentProperty, int currentOffset,
                                int originalLineNumber, String configType, String env) {
        int end = flowValueEnds.getOrDefault(originalLineNumber, -1);
        if (end < 0) end = complexValueLastLine(originalLineNumber, currentOffset);
        String value = rangeClosed(originalLineNumber, end)
                .mapToObj(lines::get)
                .map(line -> isBlank(line) ? "" : line.substring(Math.min(currentOffset, offsetIndex(line))))
                .collect(joining(LINES_SEPARATOR));

        addValue(result, currentProperty, currentOffset, originalLineNumber, null, value, configType, env);
        return end;
    }

    private int complexValueLastLine(int start, int offset) {
        int end = start;
        while (end + 1 < lines.size() && !complexValueEnd(lines.get(end + 1), offset)) end++;
        return end;
    }

    private boolean complexValueEnd(String nextLine, int currentOffset) {
        if (skip(nextLine) && !isDirective(nextLine)) return false;

        int nextOffset = offsetIndex(nextLine);
        if (currentOffset > nextOffset) return true;
        return currentOffset == nextOffset && !isComplexValue(nextLine, nextOffset);
    }

    private void parseSimpleProperty(List<Property> result,
                                     Deque<KeyOffset> currentProperty, int currentOffset,
                                     int index, String configType, String env) {
        String line = lines.get(index);
        int separatorIndex = separatorIndex(line, currentOffset);
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Incorrect delimiter in '" + line + "' in '" + new FileBasedComponent(file, index, true, configType, env) +
                    "'\nYaml property must contain ':' as delimiter.");
        }

        removePropertiesWithBiggerOffset(currentProperty, currentOffset);

        String key = line.substring(currentOffset, separatorIndex).trim();

        if (valueEmpty(line, separatorIndex)) {
            if (itsLastProperty(index, currentOffset)) {
                addValue(result, currentProperty, currentOffset, index, key, "", configType, env);
                return;
            }

            currentProperty.add(new KeyOffset(key, currentOffset, index));
            return;
        }

        String value = line.substring(separatorIndex + 1).trim();
        addValue(result, currentProperty, currentOffset, index, key, value, configType, env);
    }

    private int separatorIndex(String line, int offset) {
        return line.indexOf(':', offset);
    }

    private boolean valueEmpty(String line, int separatorIndex) {
        return isBlank(line.substring(separatorIndex + 1));
    }

    private void removePropertiesWithBiggerOffset(Deque<KeyOffset> currentProperty, int currentOffset) {
        while (!currentProperty.isEmpty() && currentProperty.peekLast().offset >= currentOffset) {
            currentProperty.pollLast();
        }
    }

    private boolean skip(String line) {
        String trim = line.trim();
        return trim.isEmpty() || isDirective(trim);
    }

    private int offsetIndex(String line) {
        return range(0, line.length())
                .filter(i -> !isWhitespace(line.charAt(i)))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("assertion error: line is empty"));
    }

    private boolean itsLastProperty(int i, int currentOffset) {
        ++i;
        while (i < lines.size()) {
            String line = lines.get(i++);
            if (skip(line)) continue;
            int offsetIndex = offsetIndex(line);
            if (currentOffset > offsetIndex) {
                return true;
            }
            if (currentOffset == offsetIndex) {
                return !isComplexValue(line, offsetIndex);
            }
            return false;
        }

        return true;
    }

    private void addValue(List<Property> result,
                          Deque<KeyOffset> currentProperty, int currentOffset, int line,
                          String lastKey, String value, String configType, String env) {
        if (lastKey != null) {
            currentProperty.add(new KeyOffset(lastKey, currentOffset, line));
        }
        int lineNumber = currentProperty.peekLast().lineNumber;
        String key = toKey(currentProperty);
        currentProperty.pollLast();
        FileBasedComponent source = fileSource(file, lineNumber, true, configType, env);
        addProperty(result, key, value, source);
    }

    private void addProperty(List<Property> result, String key, String value, FileBasedComponent source) {
        checkMultilineVarPosition(key, source);
        if (isOverrideProperty(key) && isEmptyBlock(key, value, source)) return;

        result.add(isOverrideProperty(key) ? overrideProperty(key, value, YAML, source) : property(key, value, YAML, source));
    }

    private String mergeKey(Deque<KeyOffset> currentProperty, String key) {
        return concat(
                currentProperty.stream().map(k -> k.key),
                of(key.trim())
        ).collect(joining("."));
    }

    private String toKey(Deque<KeyOffset> currentProperty) {
        return currentProperty.stream()
                .map(k -> k.key)
                .collect(joining("."));
    }

    private List<Property> envVarProperties(String configType, String environment) {
        return envBlockHeaderByVarLine().entrySet().stream()
                .map(e -> envVarProperty(e.getValue(), lines.get(e.getKey()).trim(), PROPERTIES,
                        fileSource(file, e.getKey(), false, configType, environment)))
                .collect(toList());
    }

    // '#var' lines inside '+env:' blocks (at any depth) with the header of their block, in one pass
    private Map<Integer, String> envBlockHeaderByVarLine() {
        Map<Integer, String> headerByLine = new TreeMap<>();
        Set<Integer> valueLines = literalValueLines();
        int rootOffset = rootOffset();
        String envBlockHeader = null;
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            if (valueLines.contains(lineNumber)) continue;

            String line = lines.get(lineNumber);
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            if (offsetIndex(line) > rootOffset) {
                if (envBlockHeader != null && isTempProperty(trimmed)) headerByLine.put(lineNumber, envBlockHeader);
            } else if (!isDirective(trimmed)) {
                envBlockHeader = envBlockHeader(line);
            }
        }
        return headerByLine;
    }

    // a YAML file can be indented as a whole: the root level is the offset of its first property
    private int rootOffset() {
        return lines.stream()
                .filter(line -> !skip(line))
                .mapToInt(this::offsetIndex)
                .findFirst()
                .orElse(0);
    }

    private String envBlockHeader(String topLevelLine) {
        int separatorIndex = separatorIndex(topLevelLine, 0);
        if (separatorIndex < 0 || !valueEmpty(topLevelLine, separatorIndex)) return null;

        String key = topLevelLine.substring(0, separatorIndex).trim();
        return isEnvBlock(key) ? key : null;
    }

    @RequiredArgsConstructor
    private static class KeyOffset {
        private final String key;
        private final int offset;
        private final int lineNumber;

        @Override
        public String toString() {
            return key;
        }
    }

    private record BlockScalar(String key, boolean sequence, int lineNumber, int contentOffset, int endLine, char style, String chomping, String nodeProperties) {
    }
}
