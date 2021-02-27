package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import io.microconfig.utils.CollectionUtils;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static io.microconfig.utils.CollectionUtils.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

@RequiredArgsConstructor
public class CompositePropertiesRepository implements PropertiesRepository {
    private final List<PropertiesRepository> repositories;

    public static PropertiesRepository compositeOf(List<PropertiesRepository> list, PropertiesRepository... repos) {
        return new CompositePropertiesRepository(join(list, asList(repos)));
    }

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return repositories.stream()
                .map(r -> r.getPropertiesOf(originalComponentName, environment, configType))
                .reduce(this::merge)
                .orElse(emptyMap());
    }

    private Map<String, Property> merge(Map<String, Property> one, Map<String, Property> two) {
        two.forEach(one::putIfAbsent);
        return one;
    }
}
