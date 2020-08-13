package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.Resolver;
import io.microconfig.core.properties.TypedProperties;
import io.microconfig.core.properties.templates.definition.parser.ArrowNotationParser;
import io.microconfig.core.properties.templates.definition.parser.FromToNotationParser;
import io.microconfig.core.properties.templates.definition.parser.SquareBracketsNotationParser;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.StringUtils.getExceptionMessage;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class TemplatesService {
    private final TemplatePattern templatePattern;
    private final List<TemplateDefinitionParser> templateDefinitionParsers;

    public TemplatesService() {
        this.templatePattern = defaultPattern();
        this.templateDefinitionParsers = asList(
                new FromToNotationParser(templatePattern),
                new ArrowNotationParser(templatePattern),
                new SquareBracketsNotationParser(templatePattern));
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
        List<Property> templateProperties = componentProperties.stream()
                .filter(p -> templatePattern.startsWithTemplatePrefix(p.getKey()))
                .collect(toList());
        return templateDefinitionParsers.stream()
                .map(parser -> parser.parse(templateProperties))
                .flatMap(Collection::stream)
                .collect(toList());
    }
}