package io.microconfig.configs.provider;

import io.microconfig.configs.ConfigProvider;
import io.microconfig.configs.Property;
import io.microconfig.configs.io.tree.ComponentTree;
import io.microconfig.environments.Component;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static io.microconfig.configs.io.tree.ConfigFileFilters.*;
import static io.microconfig.environments.Component.byType;
import static java.util.stream.Collectors.toList;

@RequiredArgsConstructor
public class FileBasedConfigProvider implements ConfigProvider {
    private final Set<String> configExtensions;
    private final ComponentTree componentTree;
    private final ComponentParser componentParser;

    @Override
    public Map<String, Property> getProperties(Component component, String environment) {
        return collectComponentProperties(component, environment, new LinkedHashSet<>());
    }

    private Map<String, Property> collectComponentProperties(Component component, String env, Set<Include> processedIncludes) {
        Function<Predicate<File>, List<ParsedComponent>> findAndParse = filter -> findAndParse(filter, component, env);

        List<ParsedComponent> defaultComponents = findAndParse.apply(defaultFilter(configExtensions));
        List<ParsedComponent> envSharedComponents = findAndParse.apply(envSharedFilter(configExtensions, env));
        List<ParsedComponent> envSpecificComponents = findAndParse.apply(envSpecificFilter(configExtensions, env));

        Supplier<Map<String, Property>> originalProperties = () -> {
            Map<String, Property> result = new HashMap<>();
            Consumer<List<ParsedComponent>> process = components -> components.forEach(c -> c.dumpPropertiesTo(result));

            process.accept(defaultComponents);
            process.accept(envSharedComponents);
            process.accept(envSpecificComponents);

            return result;
        };
        Supplier<Map<String, Property>> includedProperties = () -> {
            Map<String, Property> result = new HashMap<>();

            Consumer<List<ParsedComponent>> processIncludes = components -> components.stream()
                    .map(ParsedComponent::getIncludes)
                    .flatMap(Collection::stream)
                    .filter(processedIncludes::add)
                    .map(include -> collectComponentProperties(byType(include.getComponent()), include.getEnv(), processedIncludes))
                    .forEach(map -> map.forEach(result::putIfAbsent));

            processIncludes.accept(defaultComponents);
            processIncludes.accept(envSharedComponents);
            processIncludes.accept(envSpecificComponents);

            return result;
        };


        Map<String, Property> result = includedProperties.get();
        result.putAll(originalProperties.get());
        return result;
    }

    private List<ParsedComponent> findAndParse(Predicate<File> filter,
                                               Component component, String env) {
        return componentTree.getConfigFiles(component.getType(), filter)
                .map(file -> componentParser.parse(file, env))
                .collect(toList());
    }
}