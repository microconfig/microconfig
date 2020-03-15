package io.microconfig.domain.impl.properties.repository;

import io.microconfig.domain.Property;
import io.microconfig.io.formats.ConfigIoService;
import io.microconfig.io.formats.ConfigReader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static io.microconfig.domain.impl.properties.FilePropertySource.fileSource;
import static io.microconfig.domain.impl.properties.PropertyImpl.isTempProperty;
import static io.microconfig.domain.impl.properties.PropertyImpl.parse;
import static io.microconfig.io.CollectionUtils.join;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
class OriginalConfigSource {
    private final File configFile;
    private final String environment;

    public ConfigDefinition parseUsing(ConfigIoService configIo) {
        ConfigReader reader = configIo.readFrom(configFile);

        Map<Integer, String> commentByLineNumber = reader.commentsByLineNumber();
        List<Include> includes = parseIncludes(commentByLineNumber.values());
        if (containsIgnoreDirective(commentByLineNumber.values())) {
            return new ConfigDefinition(includes, emptyList());
        }

        List<Property> properties = reader.properties(environment);
        List<Property> tempProperties = parseTempProperties(commentByLineNumber);
        return new ConfigDefinition(includes, join(properties, tempProperties));
    }

    private List<Property> parseTempProperties(Map<Integer, String> commentByLineNumber) {
        return commentByLineNumber.entrySet()
                .stream()
                .filter(e -> isTempProperty(e.getValue()))
                .map(e -> parse(e.getValue(), environment, fileSource(configFile, e.getKey(), false)))
                .collect(toList());
    }

    private List<Include> parseIncludes(Collection<String> comments) {
        return comments.stream()
                .filter(IncludeDirective::isInclude)
                .map(line -> IncludeDirective.from(line).withDefaultEnv(environment))
                .flatMap(List::stream)
                .collect(toList());
    }

    private boolean containsIgnoreDirective(Collection<String> comments) {
        return comments.stream().anyMatch(s -> s.startsWith("#@Ignore"));
    }

    @RequiredArgsConstructor
    static class ConfigDefinition {
        @Getter
        private final List<Include> includes;
        private final List<Property> properties;

        public Map<String, Property> getProperties() {
            return properties.stream().collect(toMap(Property::getKey, identity()));
        }
    }
}