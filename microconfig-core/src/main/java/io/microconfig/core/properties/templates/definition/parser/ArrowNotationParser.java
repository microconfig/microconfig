package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.templates.TemplateDefinition;
import io.microconfig.core.properties.templates.TemplateDefinitionParser;
import io.microconfig.core.properties.templates.TemplatePattern;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.list;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class ArrowNotationParser implements TemplateDefinitionParser {
    private final TemplatePattern templatePattern;

    @Override
    public Collection<TemplateDefinition> parse(Collection<Property> componentProperties) {
        return componentProperties.stream()
                .map(this::processProperty)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<TemplateDefinition> processProperty(Property property) {
        if (!correctNotation(property.getKey())) return emptyList();

        String[] split = property.getValue().trim().split(" -> ");
        if (split.length != 2) return emptyList();

        if (split[0].endsWith("/*")) {
            return processWithAsterisk(property.getKey(), split[0], split[1]);
        }
        return singletonList(createTemplate(property.getKey(), split[0], split[1]));
    }

    private List<TemplateDefinition> processWithAsterisk(String key, String from, String to) {
        String fromDir = from.substring(0, from.length() - 2);
        try (Stream<Path> templates = list(new File(fromDir).toPath())) {
            return templates
                    .map(path -> createTemplate(key, path.toString(), to + "/" + path.getFileName()))
                    .collect(toList());
        } catch (IOException e) {
            throw new RuntimeException("Can't get templates from dir " + from, e);
        }
    }

    private TemplateDefinition createTemplate(String key, String from, String to) {
        TemplateDefinition templateDefinition = new TemplateDefinition(
                templatePattern.extractTemplateType(key),
                templatePattern.extractTemplateName(key),
                templatePattern);
        templateDefinition.setFromFile(from);
        templateDefinition.setToFile(to);
        return templateDefinition;
    }

    private boolean correctNotation(String key) {
        return key.endsWith(templatePattern.extractTemplateName(key)) && !key.contains("[");
    }
}
