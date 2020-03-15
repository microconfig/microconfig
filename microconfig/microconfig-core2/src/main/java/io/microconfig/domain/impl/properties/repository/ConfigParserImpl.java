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

import static io.microconfig.domain.impl.properties.FilePropertySource.fileSource;
import static io.microconfig.domain.impl.properties.PropertyImpl.isTempProperty;
import static io.microconfig.io.CollectionUtils.join;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ConfigParserImpl implements ConfigParser {
    private final ConfigIoService configIo;

    @Override
    public ConfigDefinition parse(File configFile, String env) {
        ConfigReader reader = configIo.readFrom(configFile);

        Map<Integer, String> commentByLineNumber = reader.commentsByLineNumber();
        List<Include> includes = parseIncludes(commentByLineNumber.values(), env);
        if (containsIgnoreDirective(commentByLineNumber.values())) {
            return new ConfigDefinition(includes, emptyList());
        }

        List<Property> properties = reader.properties(env);
        List<Property> tempProperties = parseTempProperties(commentByLineNumber, env, configFile);
        return new ConfigDefinition(includes, join(properties, tempProperties));
    }

    private List<Property> parseTempProperties(Map<Integer, String> commentByLineNumber, String env, File configFile) {
        return commentByLineNumber.entrySet()
                .stream()
                .filter(e -> isTempProperty(e.getValue()))
                .map(e -> PropertyImpl.parse(e.getValue(), env, fileSource(configFile, e.getKey(), false)))
                .collect(toList());
    }

    private List<Include> parseIncludes(Collection<String> comments, String env) {
        return comments.stream()
                .filter(Include::isInclude)
                .flatMap(line -> Include.parse(line, env).stream())
                .collect(toList());
    }

    private boolean containsIgnoreDirective(Collection<String> comments) {
        return comments.stream().anyMatch(s -> s.startsWith("#@Ignore"));
    }
}