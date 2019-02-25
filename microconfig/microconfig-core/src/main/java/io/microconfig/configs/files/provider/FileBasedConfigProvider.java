package io.microconfig.configs.files.provider;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.files.parser.ComponentParser;
import io.microconfig.configs.files.parser.Include;
import io.microconfig.configs.files.parser.ParsedComponent;
import io.microconfig.environments.Component;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.microconfig.configs.files.provider.ConfigFileFilters.*;
import static java.util.stream.Collectors.toMap;

public class FileBasedConfigProvider implements ConfigProvider {
    private final ComponentTree componentTree;
    private final String fileExtension;
    private final ComponentParser componentParser;

    public FileBasedConfigProvider(ComponentTree componentTree, String fileExtension, ComponentParser componentParser) {
        this.componentTree = componentTree;
        this.fileExtension = fileExtension;
        this.componentParser = componentParser;
        if (fileExtension.charAt(0) != '.') {
            throw new IllegalStateException("File extension must starts with .");
        }
    }

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return getPropertiesByKey(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> getPropertiesByKey(Component component, String environment, Set<Include> processedInclude) {
        Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> collectProperties(filter, component, environment, processedInclude);

        Map<String, Property> basicProperties = collectProperties.apply(defaultComponentFilter(fileExtension));
        Map<String, Property> envSharedProperties = collectProperties.apply(envSharedFilter(fileExtension, environment));
        Map<String, Property> envSpecificProperties = collectProperties.apply(envFilter(fileExtension, environment));

        basicProperties.putAll(envSharedProperties);
        basicProperties.putAll(envSpecificProperties);
        return basicProperties;
    }

    private Map<String, Property> collectProperties(Predicate<File> filter, Component component, String env, Set<Include> processedInclude) {
        Map<String, Property> propertyByKey = new HashMap<>();

        componentTree.getConfigFiles(component.getType(), filter)
                .map(p -> parseComponent(component, env, p))
                .forEach(c -> processComponent(c, processedInclude, propertyByKey));

        return propertyByKey;
    }

    private ParsedComponent parseComponent(Component component, String env, File file) {
        return componentParser.parse(file, component, env);
    }

    private void processComponent(ParsedComponent parsedComponent, Set<Include> processedInclude, Map<String, Property> destination) {
        Map<String, Property> includes = processIncludes(parsedComponent, processedInclude);
        Map<String, Property> componentProps = parsedComponent.getProperties().stream().collect(toMap(Property::getKey, p -> p));

        destination.putAll(includes);
        destination.putAll(componentProps);
    }

    private Map<String, Property> processIncludes(ParsedComponent parsedComponent, Set<Include> processedInclude) {
        Map<String, Property> propByKey = new HashMap<>();

        for (Include include : parsedComponent.getIncludes()) {
            if (!processedInclude.add(include)) continue;

            Map<String, Property> includedProperties = getPropertiesByKey(Component.byType(include.getComponentName()), include.getEnv(), processedInclude);
            Map<String, Property> clean = include.removeExcluded(includedProperties);
            propByKey.putAll(clean);
        }

        return propByKey;
    }
}