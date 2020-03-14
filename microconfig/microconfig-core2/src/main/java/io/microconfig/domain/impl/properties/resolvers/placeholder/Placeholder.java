package io.microconfig.domain.impl.properties.resolvers.placeholder;

import io.microconfig.domain.Resolver.Statement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static lombok.AccessLevel.PACKAGE;

@Getter
@RequiredArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(exclude = "defaultValue")
public class Placeholder implements Statement {
    private static final String SELF_REFERENCE = "this";

    @Getter
    private final int startIndex;
    @Getter
    private final int endIndex;

    private final Optional<String> configType;
    private final String component;
    private final String environment;
    private final String value;
    private final Optional<String> defaultValue;

    @Override
    public String resolve() {
        return null;
    }

    //    @Override
//    public String resolve() {
//        try {
//            Placeholder placeholder = this;
//            return hasAnotherConfigType(placeholder) ?
//                    resolveForAnotherType(placeholder, sourceOfPlaceholders.getSource(), root) :
//                    resolvePlaceholder(placeholder, sourceOfPlaceholders, root, visited);
//
//        } catch (RuntimeException e) {
//            throw new PropertyResolveException(borders.toString(), sourceOfPlaceholders, root, e);
//        }
//    }
//
//    private boolean hasAnotherConfigType(Placeholder placeholder) {
//        return placeholder.getConfigType().isPresent()
//                && !placeholder.getConfigType().get().equals(currentConfigType);
//    }
//
//    private String resolveForAnotherType(Placeholder placeholder, PropertySource source, EnvComponent root) {
//        String configType = placeholder.getConfigType().orElseThrow(IllegalStateException::new);
//        PropertyResolver resolver = resolverByType.get(configType);
//        if (resolver == null) {
//            throw new IllegalStateException("Unsupported config type '" + configType + "'. Configured types: " + resolverByType.keySet());
//        }
//
//        return resolver.resolve(tempProperty("key", placeholder.toString(), "", source), root);
//    }
//
//    private String resolvePlaceholder(Placeholder placeholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
//        Supplier<String> defaultValue = () -> placeholder.getDefaultValue()
//                .orElseThrow(() -> new PropertyResolveException(placeholder.toString(), sourceOfPlaceholder, root));
//
//        return tryResolve(placeholder, sourceOfPlaceholder, root, visited)
//                .map(value -> resolve(value, root, updateVisited(visited, placeholder)))
//                .orElseGet(defaultValue);
//    }
//
//    /**
//     * if component has a placeholder to itself, the placeholder value can be overridden.
//     * Example: 'commons' has property: java.opts = '${commons@java.opts.PermSize} ${commons@java.opts.mem}'
//     * If some component includes commons (or has placeholder to commons!) and overrides ${java.opts.PermSize} or ${java.opts.mem} - 'java.opts' will be resolved with overridden values.
//     */
//    private Optional<Property> tryResolve(Placeholder placeholder, Property sourceOfPlaceholder, EnvComponent root, Set<Placeholder> visited) {
//        boolean selfReference = placeholder.isSelfReferenced();
//        if (selfReference) {
//            placeholder = placeholder.changeComponent(sourceOfPlaceholder.getSource().getComponent().getName());
//        }
//
//        if (selfReference || canBeOverridden(placeholder, sourceOfPlaceholder)) {
//            Optional<Property> resolved = tryResolveForParents(placeholder, root, visited);
//            if (resolved.isPresent()) return resolved;
//        }
//
//        return resolveToProperty(placeholder);
//    }
//
//    private boolean canBeOverridden(Placeholder placeholder, Property sourceOfPlaceholder) {
//        boolean placeholderToTheSameComponent = placeholder.getComponent().equals(sourceOfPlaceholder.getSource().getComponent().getType())
//                && placeholder.getEnvironment().equals(sourceOfPlaceholder.getEnvContext());
//
//        return placeholderToTheSameComponent && !nonOverridableKeys.contains(placeholder.getValue());
//    }
//
//    private Optional<Property> tryResolveForParents(Placeholder placeholderToOverride, EnvComponent root, Set<Placeholder> orderedVisited) {
//        Optional<Property> forRoot = strategy.resolve(root.getComponent(), placeholderToOverride.getValue(), root.getEnvironment());
//        if (forRoot.isPresent()) return forRoot;
//
//        for (Placeholder visited : orderedVisited) {
//            Placeholder overridden = placeholderToOverride.changeComponentAndEnv(visited.getComponent(), visited.getEnvironment());
//            Optional<Property> resolved = resolveToProperty(overridden);
//            if (resolved.isPresent()) return resolved;
//        }
//
//        return empty();
//    }
//
//    //must be public for plugin
//    public Optional<Property> resolveToProperty(Placeholder placeholder) {
//        Component component = findComponent(placeholder.getComponent(), placeholder.getEnvironment());
//        return strategy.resolve(component, placeholder.getValue(), placeholder.getEnvironment());
//    }
//
//    private Component findComponent(String componentNameOrType, String env) {
//        try {
//            return environmentProvider.getByName(env)
//                    .getComponentByName(componentNameOrType)
//                    .orElse(byType(componentNameOrType));
//        } catch (EnvironmentDoesNotExistException e) {
//            return byType(componentNameOrType);
//        }
//    }
//
//    private Set<Placeholder> updateVisited(Set<Placeholder> visited, Placeholder placeholder) {
//        Set<Placeholder> updated = new LinkedHashSet<>(visited);
//        if (!updated.add(placeholder)) {
//            throw new PropertyResolveException("Found cyclic dependencies: " + updated);
//        }
//        return unmodifiableSet(updated);
//    }

    @Override
    public String toString() {
        return "${" +
                configType.map(t -> t + "::").orElse("") +
                component +
                "[" + environment + "]" +
                "@" +
                value +
                (defaultValue.map(v -> ":" + v)).orElse("") +
                "}";
    }
}