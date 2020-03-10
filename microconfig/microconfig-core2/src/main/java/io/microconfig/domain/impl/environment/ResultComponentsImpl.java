package io.microconfig.domain.impl.environment;

import io.microconfig.domain.PropertySerializer;
import io.microconfig.domain.ResultComponent;
import io.microconfig.domain.ResultComponents;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static java.util.stream.Collectors.toList;

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
        return components.stream()
                .map(c -> c.save(serializer))
                .collect(toList());
    }
}