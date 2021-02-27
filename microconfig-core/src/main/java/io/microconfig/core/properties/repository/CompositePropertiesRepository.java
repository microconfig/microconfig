package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.utils.CollectionUtils.join;
import static io.microconfig.utils.CollectionUtils.singleValue;
import static java.util.Arrays.asList;

@RequiredArgsConstructor
public class CompositePropertiesRepository implements PropertiesRepository {
    private final List<PropertiesRepository> repositories;

    public static PropertiesRepository compositeOf(List<PropertiesRepository> list, PropertiesRepository... repos) {
        List<PropertiesRepository> joined = join(list, asList(repos));
        return joined.size() == 1 ? singleValue(joined) : new CompositePropertiesRepository(joined);
    }

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return repositories.stream()
                .map(r -> r.getPropertiesOf(originalComponentName, environment, configType))
                .reduce(new LinkedHashMap<>(), this::mergeSecondToFirst);
    }

    private Map<String, Property> mergeSecondToFirst(Map<String, Property> one, Map<String, Property> two) {
        two.forEach(one::putIfAbsent);
        return one;
    }
}
