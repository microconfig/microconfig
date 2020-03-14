package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.Property;
import io.microconfig.domain.impl.properties.PropertyImpl;
import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.microconfig.domain.impl.properties.FileSource.fileSource;
import static io.microconfig.domain.impl.properties.PropertyImpl.isTempProperty;
import static io.microconfig.io.CollectionUtils.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ComponentParserImpl implements ComponentParser {
    private final ConfigIoService configIo;

    @Override
    public ParsedComponent parse(File file, String env) {
        ConfigReader reader = configIo.readFrom(file);

        Map<Integer, String> commentByLineNumber = reader.commentsByLineNumber();
        List<Include> includes = parseIncludes(commentByLineNumber.values(), env);
        if (ignoreComponent(commentByLineNumber.values())) {
            return new ParsedComponent(includes, emptyList());
        }

        List<Property> properties = reader.properties(env);
        List<Property> tempProperties = parseTempProperties(commentByLineNumber, file, env);
        return new ParsedComponent(includes, join(properties, tempProperties));
    }

    private List<Property> parseTempProperties(Map<Integer, String> comments, File file, String env) {
        return comments.entrySet()
                .stream()
                .filter(e -> isTempProperty(e.getValue()))
                .map(e -> PropertyImpl.parse(e.getValue(), env, fileSource(file, e.getKey(), false)))
                .collect(toList());
    }

    private List<Include> parseIncludes(Collection<String> comments, String env) {
        return comments.stream()
                .filter(Include::isInclude)
                .flatMap(line -> Include.parse(line, env).stream())
                .collect(toList());
    }

    private boolean ignoreComponent(Collection<String> comments) {
        return comments.stream().anyMatch(s -> s.startsWith("#@Ignore"));
    }
}