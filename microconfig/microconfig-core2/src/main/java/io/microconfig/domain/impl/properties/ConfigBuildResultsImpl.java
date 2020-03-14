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
import static io.microconfig.io.StreamUtils.toList;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class ConfigBuildResultsImpl implements ConfigBuildResults {
    private final List<ConfigBuildResult> results;

    public static ConfigBuildResults resultsOf(List<ConfigBuildResult> results) {
        return new ConfigBuildResultsImpl(results);
    }

    @Override
    public List<ConfigBuildResult> asList() {
        return results;
    }

    @Override
    public ConfigBuildResults build() {
        return resultsOf(toList(results, ConfigBuildResult::build));
    }

    @Override
    public List<Property> getProperties() {
        return flatMap(results, ConfigBuildResult::getProperties);
    }

    @Override
    public Optional<Property> getPropertyWithKey(String key) {
        return results.stream()
                .map(r -> r.getProperty(key))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    @Override
    public ConfigBuildResults forEachProperty(UnaryOperator<Property> operator) {
        return resultsOf(toList(results, r -> r.forEachProperty(operator)));
    }

    @Override
    public <T> List<T> save(PropertySerializer<T> serializer) {
        return toList(results, r -> r.save(serializer));
    }
}