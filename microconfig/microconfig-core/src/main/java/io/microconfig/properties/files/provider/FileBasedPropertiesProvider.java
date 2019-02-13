package io.microconfig.properties.files.provider;

import io.microconfig.environments.Component;
import io.microconfig.properties.PropertiesProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.files.parser.ComponentParser;
import io.microconfig.properties.files.parser.ComponentProperties;
import io.microconfig.properties.files.parser.Include;

import java.io.File;
import java.util.*;
import java.util.function.Function;

import static io.microconfig.properties.files.provider.PropertyFilter.newDefaultComponentFilter;
import static io.microconfig.properties.files.provider.PropertyFilter.newEnvComponentFilter;
import static io.microconfig.utils.CollectionUtils.join;
import static java.util.stream.Collectors.toMap;

public class FileBasedPropertiesProvider implements PropertiesProvider {
    private final ComponentTree componentTree;
    private final String fileExtension;
    private final ComponentParser<File> componentParser;

    public FileBasedPropertiesProvider(ComponentTree componentTree, String fileExtension, ComponentParser<File> componentParser) {
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
        Function<PropertyFilter, Map<String, Property>> collectProperties = filter -> collectProperties(filter, component, environment, processedInclude);

        Map<String, Property> basicProperties = collectProperties.apply(newDefaultComponentFilter(fileExtension));
        Map<String, Property> envSpecificProperties = collectProperties.apply(newEnvComponentFilter(environment, fileExtension));

        return join(basicProperties, envSpecificProperties);
    }

    private Map<String, Property> collectProperties(PropertyFilter filter, Component component, String env, Set<Include> processedInclude) {
        Map<String, Property> propertyByKey = new HashMap<>();

        componentTree.getPropertyFiles(component.getType(), filter)
                .map(p -> parseComponentProperties(component, env, p))
                .forEach(c -> processComponent(c, processedInclude, propertyByKey));

        return propertyByKey;
    }

    private ComponentProperties parseComponentProperties(Component component, String env, File file) {
        return componentParser.parse(file, component, env);
    }

    private void processComponent(ComponentProperties componentProperties, Set<Include> processedInclude, Map<String, Property> destination) {
        Map<String, Property> includes = processIncludes(componentProperties, processedInclude);
        Map<String, Property> componentProps = componentProperties.getProperties().stream().collect(toMap(Property::getKey, p -> p));

        destination.putAll(includes);
        destination.putAll(componentProps);
    }

    private Map<String, Property> processIncludes(ComponentProperties componentProperties, Set<Include> processedInclude) {
        Map<String, Property> propByKey = new HashMap<>();

        for (Include include : componentProperties.getIncludes()) {
            if (!processedInclude.add(include)) continue;

            Map<String, Property> includedProperties = getPropertiesByKey(Component.byType(include.getComponentName()), include.getEnv(), processedInclude);
            Map<String, Property> clean = include.removeExcluded(includedProperties);
            propByKey.putAll(clean);
        }

        return propByKey;
    }
}