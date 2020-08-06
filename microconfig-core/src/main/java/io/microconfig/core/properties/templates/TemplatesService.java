package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.properties.templates.definition.parser.ArrowNotationParser;
import io.microconfig.core.properties.templates.definition.parser.FromToNotationParser;
import io.microconfig.core.properties.templates.definition.parser.TemplateDefinitionParser;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.UnaryOperator;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.StringUtils.getExceptionMessage;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class TemplatesService {
    private final TemplatePattern templatePattern;

    public TemplatesService() {
        this.templatePattern = defaultPattern();
    }

    public static UnaryOperator<TypedProperties> resolveTemplatesBy(Resolver resolver) {
        TemplatesService templatesService = new TemplatesService();
        return tp -> {
            templatesService.resolveTemplate(tp, resolver);
            return tp.without(p -> templatesService.templatePattern.startsWithTemplatePrefix(p.getKey()));
        };
    }

    public void resolveTemplate(TypedProperties properties, Resolver resolver) {
        Collection<TemplateDefinition> templateDefinitions = findTemplateDefinitionsFrom(properties.getProperties());
        System.out.println(properties.getDeclaringComponent() + " " + templateDefinitions);
        templateDefinitions.forEach(def -> {
            try {
                def.resolveAndCopy(resolver, properties);
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "Template error: " + def +
                                "\nComponent: " + properties.getDeclaringComponent() +
                                "\n" + getExceptionMessage(e), e
                );
            }
        });
    }

    //todo test exception handling
    private Collection<TemplateDefinition> findTemplateDefinitionsFrom(Collection<Property> componentProperties) {
        List<TemplateDefinitionParser> templateDefinitionParsers = getTemplateDefinitionParsers();
        Map<String, TemplateDefinition> templateByName = new LinkedHashMap<>();

        componentProperties.stream()
                .filter(p -> templatePattern.startsWithTemplatePrefix(p.getKey()))
                .forEach(p -> templateDefinitionParsers.forEach(parser -> parser.add(p.getKey(), p.getValue())));

        templateDefinitionParsers.stream()
                .map(TemplateDefinitionParser::getDefinitions)
                .flatMap(Collection::stream)
                .forEach(definition -> templateByName.put(definition.getTemplateName(), definition));
        return templateByName.values();
    }

    private List<TemplateDefinitionParser> getTemplateDefinitionParsers() {
        return asList(new FromToNotationParser(templatePattern), new ArrowNotationParser(templatePattern));
    }
}