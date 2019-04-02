package io.microconfig.configs.resolver.placeholder;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolveException;
import io.microconfig.configs.resolver.PropertyResolver;
import io.microconfig.environments.Component;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import static io.microconfig.environments.Component.byType;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class PlaceholderResolver implements PropertyResolver {
    private final EnvironmentProvider environmentProvider;
    private final PlaceholderResolveStrategy resolveStrategy;
    private final Set<String> nonOverridableKeys;

    @Override
    public String resolve(Property sourceOfPlaceholders, EnvComponent root) {
        return doResolve(sourceOfPlaceholders, root, emptySet());
    }

    private String doResolve(Property sourceOfPlaceholders, EnvComponent root, Set<Placeholder> visited) {
        StringBuilder currentPropertyValue = new StringBuilder(sourceOfPlaceholders.getValue());

        while (true) {
            Matcher matcher = Placeholder.placeholderMatcher(currentPropertyValue);
            if (!matcher.find()) break;

            Placeholder placeholder = newPlaceholder(matcher.group(), sourceOfPlaceholders, root, visited);
            Optional<Property> resolvedProperty = resolveToProperty(placeholder);
            String resolvedValue = resolveValue(resolvedProperty, root, placeholder, visited, sourceOfPlaceholders);

            currentPropertyValue.replace(matcher.start(), matcher.end(), resolvedValue);
        }

        return currentPropertyValue.toString();
    }

    private Placeholder newPlaceholder(String innerPlaceholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
        try {
            Placeholder placeholder = Placeholder.parse(innerPlaceholder, sourceOfPlaceholder.getEnvContext());
            return tryOverride(placeholder, sourceOfPlaceholder, root, visited);
        } catch (IllegalArgumentException e) {
            throw new PropertyResolveException(innerPlaceholder, sourceOfPlaceholder, root, e);
        }
    }

    /**
     * 1) if component has a placeholder to himself, the placeholder value can be overriden.
     * Example: commons has prop java.opts = '${commons@java.opts.PermSize} ${commons@java.opts.mem}'
     * If some component includes commons(or has placeholder to commons!!!) and overrides ${java.opts.PermSize} or ${java.opts.mem} - java.opts will be resolved with overriden values.
     * 2) Also 'this' componentName is replaced with root component name.
     */
    private Placeholder tryOverride(Placeholder placeholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
        boolean selfReference = placeholder.isSelfReferenced();
        if (selfReference) {
            placeholder = placeholder.changeComponent(sourceOfPlaceholder.getSource().getComponent().getName());
        }

        if (selfReference || (placeholderToTheSameComponent(placeholder, sourceOfPlaceholder) && !nonOverridableKeys.contains(placeholder.getValue()))) {
            Optional<Placeholder> overriden = tryOverrideForRoot(placeholder, root);
            if (!overriden.isPresent()) overriden = tryOverrideForParent(placeholder, visited);
            if (overriden.isPresent()) placeholder = overriden.get();
        }

        return placeholder;
    }

    private boolean placeholderToTheSameComponent(Placeholder placeholder, Property sourceOfPlaceholder) {
        return placeholder.getComponent().equals(sourceOfPlaceholder.getSource().getComponent().getType())
                && placeholder.getEnvironment().equals(sourceOfPlaceholder.getEnvContext());
    }

    private Optional<Placeholder> tryOverrideForRoot(Placeholder placeholder, EnvComponent root) {
        return resolveStrategy.resolve(root.getComponent(), placeholder.getValue(), root.getEnvironment()).isPresent() ?
                of(placeholder.changeComponent(root.getComponent().getName(), root.getEnvironment()))
                : empty();
    }

    /**
     * @param placeholderToOverride - placeholder to override
     * @param orderedVisited        - last placeholder in the set - last visited
     */
    private Optional<Placeholder> tryOverrideForParent(Placeholder placeholderToOverride, Set<Placeholder> orderedVisited) {
        if (orderedVisited.isEmpty()) return empty();

        for (Placeholder placeholder : orderedVisited) {
            Component component = getComponentByName(placeholder.getComponent(), placeholder.getEnvironment());
            Optional<Placeholder> overrider = tryOverrideForRoot(placeholderToOverride, new EnvComponent(component, placeholder.getEnvironment()));
            if (overrider.isPresent()) return overrider;
        }

        return empty();
    }

    //must be public for plugin
    public Optional<Property> resolveToProperty(Placeholder placeholder) {
        Component component = getComponentByName(placeholder.getComponent(), placeholder.getEnvironment());
        return resolveStrategy.resolve(component, placeholder.getValue(), placeholder.getEnvironment());
    }

    private Component getComponentByName(String componentName, String env) {
        try {
            return environmentProvider.getByName(env)
                    .getComponentByName(componentName)
                    .orElse(byType(componentName));
        } catch (EnvironmentNotExistException e) {
            return byType(componentName);
        }
    }

    private String resolveValue(Optional<Property> property, EnvComponent root, Placeholder placeholder, Set<Placeholder> visited, Property sourceOfPlaceholders) {
        if (property.isPresent()) {
            return doResolve(property.get(), root, updateVisited(visited, placeholder));
        }

        return placeholder.getDefaultValue()
                .orElseThrow(() -> new PropertyResolveException(placeholder.toString(), sourceOfPlaceholders, root));
    }

    private Set<Placeholder> updateVisited(Set<Placeholder> visited, Placeholder placeholder) {
        Set<Placeholder> updated = new LinkedHashSet<>(visited);
        if (!updated.add(placeholder)) {
            throw new PropertyResolveException("Found cyclic dependencies: " + updated);
        }
        return unmodifiableSet(updated);
    }
}