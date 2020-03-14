package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static io.microconfig.io.StreamUtils.flatMap;
import static io.microconfig.io.StreamUtils.map;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class CompositeConfigBuildResult implements ConfigBuildResults {
    private final List<ConfigBuildResult> results;

    public static ConfigBuildResults composite(List<ConfigBuildResult> results) {
        return new CompositeConfigBuildResult(results);
    }

    @Override
    public List<ConfigBuildResult> asList() {
        return results;
    }

    @Override
    public ConfigBuildResults build() {
        return composite(map(results, ConfigBuildResult::build));
    }

    @Override
    public List<Property> getProperties() {
        return flatMap(results, ConfigBuildResult::getProperties);
    }

    @Override
    public Optional<Property> getProperty(String key) {
        return results.stream()
                .map(r -> r.getProperty(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public ConfigBuildResults forEachProperty(UnaryOperator<Property> operator) {
        return composite(map(results, r -> r.forEachProperty(operator)));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return map(results, r -> r.save(serializer));
    }
}