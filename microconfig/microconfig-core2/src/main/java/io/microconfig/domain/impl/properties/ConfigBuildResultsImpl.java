package io.microconfig.domain.impl.properties;

import io.microconfig.domain.ConfigBuildResult;
import io.microconfig.domain.ConfigBuildResults;
import io.microconfig.domain.Property;
import io.microconfig.domain.PropertySerializer;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.UnaryOperator;

import static io.microconfig.utils.StreamUtils.map;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ConfigBuildResultsImpl implements ConfigBuildResults {
    private final List<ConfigBuildResult> results;

    public static ConfigBuildResults composite(List<ConfigBuildResult> results) {
        return new ConfigBuildResultsImpl(results);
    }

    @Override
    public List<ConfigBuildResult> asList() {
        return results;
    }

    @Override
    public ConfigBuildResult first() {
        return results.get(0);
    }

    @Override
    public ConfigBuildResults forEachProperty(UnaryOperator<Property> operator) {
        return composite(map(results, r -> r.applyForEachProperty(operator)));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return map(results, r -> r.save(serializer));
    }
}