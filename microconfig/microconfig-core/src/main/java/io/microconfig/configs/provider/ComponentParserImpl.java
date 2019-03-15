package io.microconfig.configs.provider;

import io.microconfig.configs.Property;
import io.microconfig.configs.io.ioservice.ConfigIoService;
import io.microconfig.configs.io.ioservice.ConfigReader;
import io.microconfig.configs.sources.FileSource;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.microconfig.configs.Property.isTempProperty;
import static io.microconfig.utils.CollectionUtils.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentParserImpl implements ComponentParser {
    private final ConfigIoService configIo;

    @Override
    public ParsedComponent parse(File file, String env) {
        ConfigReader reader = configIo.read(file);
        Map<Integer, String> comments = reader.commentsByLineNumber();
        List<Property> properties = reader.properties(env);

        List<Property> tempProperties = parseTempProperties(comments, file, env);
        List<Include> includes = parseIncludes(comments.values(), env);
        boolean ignore = shouldIgnore(comments.values());

        return new ParsedComponent(includes, ignore ? emptyList() : join(properties, tempProperties));
    }

    private List<Property> parseTempProperties(Map<Integer, String> comments, File file, String env) {
        return comments
                .entrySet()
                .stream()
                .filter(e -> isTempProperty(e.getValue()))
                .map(e -> Property.parse(e.getValue(), env, FileSource.fileSource(file, e.getKey(), false)))
                .collect(toList());
    }

    private List<Include> parseIncludes(Collection<String> comments, String env) {
        return comments.stream()
                .filter(Include::isInclude)
                .flatMap(line -> Include.parse(line, env).stream())
                .collect(toList());
    }

    private boolean shouldIgnore(Collection<String> comments) {
        return comments.stream()
                .anyMatch(s -> s.startsWith("#@Ignore"));
    }
}