package io.microconfig.properties.resolver.placeholder;

import io.microconfig.environments.Component;
import io.microconfig.environments.EnvironmentNotExistException;
import io.microconfig.environments.EnvironmentProvider;
import io.microconfig.properties.Property;
import io.microconfig.properties.PropertySource;
import io.microconfig.properties.resolver.EnvComponent;
import io.microconfig.properties.resolver.PropertyResolveException;
import io.microconfig.properties.resolver.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import static io.microconfig.environments.Component.byType;
import static io.microconfig.properties.Property.tempProperty;
import static io.microconfig.properties.resolver.placeholder.Placeholder.parse;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.empty;

@RequiredArgsConstructor
public class PlaceholderResolver implements PropertyResolver {
    private final EnvironmentProvider environmentProvider;
    private final PlaceholderResolveStrategy strategy;
    private final Set<String> nonOverridableKeys;

    private final String currentConfigType;
    private final Map<String, PropertyResolver> resolverByType;

    @Override
    public String resolve(Property sourceOfPlaceholders, EnvComponent root) {
        return doResolve(sourceOfPlaceholders, root, emptySet());
    }

    private String doResolve(Property sourceOfPlaceholders, EnvComponent root, Set<Placeholder> visited) {
        StringBuilder resultValue = new StringBuilder(sourceOfPlaceholders.getValue());

        while (true) {
            Matcher matcher = Placeholder.matcher(resultValue);
            if (!matcher.find()) break;

            String resolved = doResolve(matcher.group(), sourceOfPlaceholders, root, visited);
            resultValue.replace(matcher.start(), matcher.end(), resolved);
        }

        return resultValue.toString();
    }

    private String doResolve(String value, Property sourceOfPlaceholders, EnvComponent root, Set<Placeholder> visited) {
        try {
            Placeholder placeholder = parse(value, sourceOfPlaceholders.getEnvContext());
            if (hasAnotherConfigType(placeholder)) {
                return resolveForAnotherType(placeholder, sourceOfPlaceholders.getSource(), root);
            }

            return resolvePlaceholder(placeholder, sourceOfPlaceholders, root, visited);
        } catch (PropertyResolveException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new PropertyResolveException(value, sourceOfPlaceholders, root, e);
        }
    }

    private boolean hasAnotherConfigType(Placeholder placeholder) {
        return placeholder.getConfigType().isPresent()
                && !placeholder.getConfigType().get().equals(currentConfigType);
    }

    private String resolveForAnotherType(Placeholder placeholder, PropertySource source, EnvComponent root) {
        String configType = placeholder.getConfigType().orElseThrow(IllegalStateException::new);
        PropertyResolver resolver = resolverByType.get(configType);
        if (resolver == null) {
            throw new IllegalStateException("Unsupported config type '" + configType + "'. Configured types: " + resolverByType.keySet());
        }

        return resolver.resolve(tempProperty("key", placeholder.toString(), "", source), root);
    }

    private String resolvePlaceholder(Placeholder placeholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
        Supplier<String> defaultValue = () -> placeholder.getDefaultValue()
                .orElseThrow(() -> new PropertyResolveException(placeholder.toString(), sourceOfPlaceholder, root));

        return tryResolve(placeholder, sourceOfPlaceholder, root, visited)
                .map(value -> doResolve(value, root, updateVisited(visited, placeholder)))
                .orElseGet(defaultValue);
    }

    /**
     * if component has a placeholder to itself, the placeholder value can be overridden.
     * Example: 'commons' has property: java.opts = '${commons@java.opts.PermSize} ${commons@java.opts.mem}'
     * If some component includes commons (or has placeholder to commons!) and overrides ${java.opts.PermSize} or ${java.opts.mem} - 'java.opts' will be resolved with overridden values.
     */
    private Optional<Property> tryResolve(Placeholder placeholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
        boolean selfReference = placeholder.isSelfReferenced();
        if (selfReference) {
            placeholder = placeholder.changeComponent(sourceOfPlaceholder.getSource().getComponent().getName());
        }

        if (selfReference || canBeOverridden(placeholder, sourceOfPlaceholder)) {
            Optional<Property> resolved = tryResolveForParents(placeholder, root, visited);
            if (resolved.isPresent()) return resolved;
        }

        return resolveToProperty(placeholder);
    }

    private boolean canBeOverridden(Placeholder placeholder, Property sourceOfPlaceholder) {
        Supplier<Boolean> placeholderToTheSameComponent = () -> placeholder.getComponent().equals(sourceOfPlaceholder.getSource().getComponent().getType())
                && placeholder.getEnvironment().equals(sourceOfPlaceholder.getEnvContext()
        );

        return placeholderToTheSameComponent.get() && !nonOverridableKeys.contains(placeholder.getValue());
    }

    private Optional<Property> tryResolveForParents(Placeholder placeholderToOverride, EnvComponent root, Set<Placeholder> orderedVisited) {
        Optional<Property> forRoot = strategy.resolve(root.getComponent(), placeholderToOverride.getValue(), root.getEnvironment());
        if (forRoot.isPresent()) return forRoot;

        for (Placeholder visited : orderedVisited) {
            Placeholder overridden = placeholderToOverride.changeComponentAndEnv(visited.getComponent(), visited.getEnvironment());
            Optional<Property> resolved = resolveToProperty(overridden);
            if (resolved.isPresent()) return resolved;
        }

        return empty();
    }

    //must be public for plugin
    public Optional<Property> resolveToProperty(Placeholder placeholder) {
        Component component = findComponent(placeholder.getComponent(), placeholder.getEnvironment());
        return strategy.resolve(component, placeholder.getValue(), placeholder.getEnvironment());
    }

    private Component findComponent(String componentNameOrType, String env) {
        try {
            return environmentProvider.getByName(env)
                    .getComponentByName(componentNameOrType)
                    .orElse(byType(componentNameOrType));
        } catch (EnvironmentNotExistException e) {
            return byType(componentNameOrType);
        }
    }

    private Set<Placeholder> updateVisited(Set<Placeholder> visited, Placeholder placeholder) {
        Set<Placeholder> updated = new LinkedHashSet<>(visited);
        if (!updated.add(placeholder)) {
            throw new PropertyResolveException("Found cyclic dependencies: " + updated);
        }
        return unmodifiableSet(updated);
    }
}