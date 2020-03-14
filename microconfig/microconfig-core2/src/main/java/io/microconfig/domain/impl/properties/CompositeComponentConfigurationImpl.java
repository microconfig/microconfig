package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.io.StreamUtils.flatMapEach;
import static io.microconfig.io.StreamUtils.forEach;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class CompositeComponentConfigurationImpl implements CompositeComponentConfiguration {
    private final List<ComponentConfiguration> results;

    public static CompositeComponentConfiguration resultsOf(List<ComponentConfiguration> results) {
        return new CompositeComponentConfigurationImpl(results);
    }

    @Override
    public List<ComponentConfiguration> asList() {
        return results;
    }

    @Override
    public CompositeComponentConfiguration resolveBy(Resolver resolver) {
        return resultsOf(forEach(results, r -> r.resolveBy(resolver)));
    }

    @Override
    public List<Property> getProperties() {
        return flatMapEach(results, ComponentConfiguration::getProperties);
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return results.stream()
                .map(r -> r.getPropertyWithKey(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return forEach(results, r -> r.save(serializer));
    }
}