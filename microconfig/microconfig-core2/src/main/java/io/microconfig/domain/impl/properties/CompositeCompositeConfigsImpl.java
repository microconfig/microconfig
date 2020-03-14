package io.microconfig.domain.impl.properties;

import io.microconfig.domain.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static io.microconfig.io.StreamUtils.flatMapEach;
import static io.microconfig.io.StreamUtils.forEach;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class CompositeCompositeConfigsImpl implements CompositeCompositeConfigs {
    private final List<ComponentConfigs> results;

    public static CompositeCompositeConfigs resultsOf(List<ComponentConfigs> results) {
        return new CompositeCompositeConfigsImpl(results);
    }

    @Override
    public List<ComponentConfigs> asList() {
        return results;
    }

    @Override
    public CompositeCompositeConfigs resolveBy(Resolver resolver) {
        return resultsOf(forEach(results, r -> r.resolveBy(resolver)));
    }

    @Override
    public List<Property> getProperties() {
        return flatMapEach(results, ComponentConfigs::getProperties);
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