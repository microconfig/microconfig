package io.microconfig.configs.files.provider;

import io.microconfig.commands.factory.ConfigType;
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
import static io.microconfig.environments.Component.byType;

public class FileBasedConfigProvider implements ConfigProvider {
    private final ComponentTree componentTree;
    private final Set<String> configExtensions;
    private final ComponentParser componentParser;

    public FileBasedConfigProvider(ComponentTree componentTree, ConfigType configType, ComponentParser componentParser) {
        this.componentTree = componentTree;
        this.configExtensions = configType.getConfigExtensions();
        this.componentParser = componentParser;
    }

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return collectProperties(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> collectProperties(Component component, String env, Set<Include> processedInclude) {
        Function<Predicate<File>, Map<String, Property>> collectProperties = filter -> collectProperties(filter, component, env, processedInclude);

        Map<String, Property> basicProperties = collectProperties.apply(defaultComponentFilter(configExtensions));
        Map<String, Property> envSharedProperties = collectProperties.apply(envSharedFilter(configExtensions, env));
        Map<String, Property> envSpecificProperties = collectProperties.apply(envSpecificFilter(configExtensions, env));

        basicProperties.putAll(envSharedProperties);
        basicProperties.putAll(envSpecificProperties);
        return basicProperties;
    }

    private Map<String, Property> collectProperties(Predicate<File> filter, Component component, String env, Set<Include> processedInclude) {
        Map<String, Property> propertyByKey = new HashMap<>();

        componentTree.getConfigFiles(component.getType(), filter)
                .map(file -> componentParser.parse(file, env))
                .forEach(c -> processComponent(c, processedInclude, propertyByKey));

        return propertyByKey;
    }

    private void processComponent(ParsedComponent parsedComponent, Set<Include> processedInclude, Map<String, Property> destination) {
        Map<String, Property> included = processIncludes(parsedComponent, processedInclude);
        Map<String, Property> properties = parsedComponent.getPropertiesAsMas();

        destination.putAll(included);
        destination.putAll(properties);
    }

    private Map<String, Property> processIncludes(ParsedComponent parsedComponent, Set<Include> processedInclude) {
        Map<String, Property> result = new HashMap<>();

        for (Include include : parsedComponent.getIncludes()) {
            if (!processedInclude.add(include)) continue;

            Map<String, Property> included = collectProperties(byType(include.getComponent()), include.getEnv(), processedInclude);
            result.putAll(included);
        }

        return result;
    }
}