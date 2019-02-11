package io.microconfig.configs.properties.files.provider;

import io.microconfig.configs.environment.Component;
import io.microconfig.configs.properties.PropertiesProvider;
import io.microconfig.configs.properties.Property;
import io.microconfig.configs.properties.files.parser.ComponentParser;
import io.microconfig.configs.properties.files.parser.ComponentProperties;
import io.microconfig.configs.properties.files.parser.Include;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;

import static deployment.util.CollectionUtils.join;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class FileBasedPropertiesProvider implements PropertiesProvider {
    private static final int TWO_PART_FILENAME = 2;
    private static final int THREE_PART_FILENAME = 3;

    private final ComponentTree componentTree;
    private final String fileExtension;
    private final ComponentParser<File> componentParser;

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return getPropertiesByKey(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> getPropertiesByKey(Component component, String environment, Set<Include> processedInclude) {
        Map<String, Property> basicProperties = collectProperties(newComponentFilter(component.getType()), component, environment, processedInclude);
        Map<String, Property> envSpecificProperties = collectProperties(newEnvComponentFilter(component.getType(), environment), component, environment, processedInclude);

        return join(basicProperties, envSpecificProperties);
    }

    private Map<String, Property> collectProperties(PropertyFileNameFilter filter, Component component, String env, Set<Include> processedInclude) {
        Map<String, Property> propertyByKey = new TreeMap<>();

        componentTree.getPropertyFiles(filter.getComponentType(), filter)
                .map(p -> parseComponentProperties(component, env, p))
                .map(c -> processComponent(c, processedInclude))
                .forEach(propertyByKey::putAll);

        return propertyByKey;
    }

    private ComponentProperties parseComponentProperties(Component component, String env, File file) {
        return componentParser.parse(file, component, env);
    }

    private Map<String, Property> processComponent(ComponentProperties componentProperties, Set<Include> processedInclude) {
        Map<String, Property> includes = processIncludes(componentProperties, processedInclude);
        Map<String, Property> componentProps = componentProperties.getProperties().stream().collect(toMap(Property::getKey, p -> p));

        return join(includes, componentProps);
    }

    private Map<String, Property> processIncludes(ComponentProperties componentProperties, Set<Include> processedInclude) {
        Map<String, Property> propByKey = new HashMap<>();

        componentProperties.getIncludes().forEach(include -> {
            if (!processedInclude.add(include)) return;

            Map<String, Property> includedProperties = getPropertiesByKey(Component.byType(include.getComponentName()), include.getEnv(), processedInclude);
            Map<String, Property> clean = include.removeExcluded(includedProperties);
            propByKey.putAll(clean);
        });

        return propByKey;
    }

    private PropertyFileNameFilter newComponentFilter(String componentType) {
        return new PropertyFileNameFilter(componentType, fileExtension, TWO_PART_FILENAME);
    }

    private PropertyFileNameFilter newEnvComponentFilter(String componentType, String environment) {
        return new PropertyFileNameFilter(componentType, environment + "." + fileExtension, THREE_PART_FILENAME);
    }
}