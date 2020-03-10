package io.microconfig.domain.impl.properties;

import io.microconfig.domain.PropertySerializer;
import io.microconfig.domain.ResultComponent;
import io.microconfig.domain.ResultComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static io.microconfig.utils.StreamUtils.map;

@RequiredArgsConstructor
public class ResultComponentsImpl implements ResultComponents {
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
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return map(components, c -> c.save(serializer));
    }
}