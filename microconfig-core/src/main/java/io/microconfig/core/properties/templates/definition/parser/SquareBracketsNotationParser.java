package io.microconfig.core.properties.templates.definition.parser;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.templates.TemplateDefinition;
import io.microconfig.core.properties.templates.TemplateDefinitionParser;
import io.microconfig.core.properties.templates.TemplatePattern;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;

@RequiredArgsConstructor
public class SquareBracketsNotationParser implements TemplateDefinitionParser {
    private final TemplatePattern templatePattern;

    @Override
    public Collection<TemplateDefinition> parse(Collection<Property> componentProperties) {
        return componentProperties.stream()
                .map(this::processProperty)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<TemplateDefinition> processProperty(Property property) {
        if (!correctNotation(property)) return emptyList();

        String[] split = property.getValue().trim().split(" -> ");
        if (split.length != 2) return emptyList();

        Matcher matcher = compile(".*\\.\\[(.*)]").matcher(property.getKey());
        if (!matcher.matches()) return emptyList();

        String templateType = templatePattern.extractTemplateType(property.getKey());
        return of(matcher.group(1).split(","))
                .map(templateName -> createTemplateDefinition(templateType, templateName, split[0], split[1]))
                .collect(Collectors.toList());
    }

    private boolean correctNotation(Property property) {
        return property.getKey().contains("[")
                && property.getKey().endsWith(templatePattern.extractTemplateName(property.getKey()))
                && !property.getValue().contains("*");
    }

    private TemplateDefinition createTemplateDefinition(String templateType, String templateName, String fromFile, String toFile) {
        TemplateDefinition templateDefinition = new TemplateDefinition(templateType, templateName, templatePattern);
        templateDefinition.setFromFile(fromFile);
        templateDefinition.setToFile(toFile);
        return templateDefinition;
    }
}
