package io.microconfig.domain.impl.properties;

import io.microconfig.domain.Component;
import io.microconfig.domain.Components;
import io.microconfig.domain.CompositeComponentProperties;
import io.microconfig.domain.ConfigTypeFilter;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static io.microconfig.domain.impl.properties.ComponentsCollector.toComponents;
import static io.microconfig.domain.impl.properties.CompositeComponentPropertiesImpl.resultsOf;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
@EqualsAndHashCode
public class ComponentsImpl implements Components {
    private final List<Component> components;

    @Override
    public List<Component> asList() {
        return components;
    }

    @Override
    public Components filterComponents(List<String> names) {
        if (names.isEmpty()) return this;

        Map<String, Component> componentByName = components.stream().collect(toMap(Component::getName, identity()));
        return names.stream()
                .map(name -> requireNonNull(componentByName.get(name), () -> notFoundComponentMessage(name)))
                .collect(toComponents());
    }

    @Override
    public CompositeComponentProperties getPropertiesFor(ConfigTypeFilter filter) {
        return resultsOf(
                components.stream()
                        .map(c -> c.getPropertiesFor(filter))
                        .map(CompositeComponentProperties::asList)
                        .flatMap(List::stream)
                        .collect(toList())
        );
    }

    private String notFoundComponentMessage(String component) {
        return "Component '" + component + "' is not found";
    }

    @Override
    public String toString() {
        return components.toString();
    }

}