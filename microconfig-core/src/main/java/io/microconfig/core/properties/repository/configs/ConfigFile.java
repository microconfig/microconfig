package io.microconfig.core.properties.repository.configs;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ConfigIo;
import io.microconfig.core.properties.io.ConfigReader;
import io.microconfig.core.properties.repository.ConfigFileParser.ConfigDefinition;
import io.microconfig.core.properties.repository.Include;
import io.microconfig.core.properties.repository.Includes;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.FileBasedComponent.fileSource;
import static io.microconfig.core.properties.PropertyImpl.isTempProperty;
import static io.microconfig.core.properties.PropertyImpl.parse;
import static io.microconfig.utils.StreamUtils.toLinkedMap;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

@RequiredArgsConstructor
class ConfigFile {
    private final File file;
    private final String configType;
    private final String environment;

    public ConfigDefinition parseUsing(ConfigIo configIo) {
        ConfigReader reader = configIo.readFrom(file);

        Map<Integer, String> commentByLineNumber = reader.commentsByLineNumber();
        List<Include> includes = parseIncludes(commentByLineNumber.values());
        if (containsIgnoreDirective(commentByLineNumber.values())) {
            return new ConfigDefinition(includes, emptyMap());
        }

        List<Property> properties = reader.properties(configType, environment);
        List<Property> tempProperties = parseTempProperties(commentByLineNumber);
        return new ConfigDefinition(includes, joinToMap(properties, tempProperties));
    }

    private List<Property> parseTempProperties(Map<Integer, String> commentByLineNumber) {
        return commentByLineNumber.entrySet()
                .stream()
                .filter(e -> isTempProperty(e.getValue()))
                .map(e -> parse(e.getValue(), PROPERTIES, fileSource(file, e.getKey(), false, configType, environment)))
                .collect(toList());
    }

    private List<Include> parseIncludes(Collection<String> comments) {
        return comments.stream()
                .filter(Includes::isInclude)
                .map(line -> Includes.from(line).withDefaultEnv(environment))
                .flatMap(List::stream)
                .collect(toList());
    }

    private boolean containsIgnoreDirective(Collection<String> comments) {
        return comments.stream().anyMatch(s -> s.startsWith("#@Ignore"));
    }

    private Map<String, Property> joinToMap(List<Property> properties, List<Property> tempProperties) {
        return concat(properties.stream(), tempProperties.stream())
                .collect(toLinkedMap(Property::getKey, identity()));
    }
}