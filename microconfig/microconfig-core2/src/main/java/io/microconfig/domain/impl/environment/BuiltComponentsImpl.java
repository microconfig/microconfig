package io.microconfig.domain.impl.environment;

import io.microconfig.domain.PropertySerializer;
import io.microconfig.domain.ResultComponent;
import io.microconfig.domain.ResultComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class BuiltComponentsImpl implements ResultComponents {
    private final List<ResultComponent> components;

    @Override
    public List<ResultComponent> asList() {
        return components;
    }

    @Override
    public ResultComponent first() {
        return components.get(0);
    }

    @Override
    public <T> T save(PropertySerializer<T> serializer) {
        return null;
    }
}