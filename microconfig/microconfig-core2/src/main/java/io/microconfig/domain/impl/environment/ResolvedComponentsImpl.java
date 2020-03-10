package io.microconfig.domain.impl.environment;

import io.microconfig.domain.PropertySerializer;
import io.microconfig.domain.ResolvedComponent;
import io.microconfig.domain.ResolvedComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ResolvedComponentsImpl implements ResolvedComponents {
    private final List<ResolvedComponent> resolvedComponents;

    @Override
    public List<ResolvedComponent> asList() {
        return resolvedComponents;
    }

    @Override
    public ResolvedComponent first() {
        return resolvedComponents.get(0);
    }

    @Override
    public <T> T serialize(PropertySerializer<T> serializer) {
        return null;
    }
}
