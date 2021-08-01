package io.microconfig.core.properties.repository;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentRepository;
import io.microconfig.core.exceptions.MicroconfigException;
import io.microconfig.core.properties.PropertiesRepository;
import io.microconfig.core.properties.Property;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.microconfig.core.configtypes.ConfigTypeFilters.configType;
import static java.util.Collections.emptyList;

@RequiredArgsConstructor
public class EnvProfilesPropertiesRepository implements PropertiesRepository {
    private final EnvironmentRepository environments;

    @Override
    public Map<String, Property> getPropertiesOf(String originalComponentName, String environment, ConfigType configType) {
        return getProfilesFor(environment, originalComponentName)
                .stream()
                .map(c -> c.getPropertiesFor(configType(configType)).getPropertiesAsMap())
                .reduce(new LinkedHashMap<>(), this::mergeSecondToFirst);
    }

    //todo rewrite
    private List<Component> getProfilesFor(String envName, String originalComponentName) {
        try {
            Environment env = environments.getByName(envName);
            env.getComponentWithName(originalComponentName); //component must be top level
            return env.getProfiles();
        } catch (MicroconfigException ignore) {
            return emptyList();
        }
    }

    private Map<String, Property> mergeSecondToFirst(Map<String, Property> one, Map<String, Property> two) {
        two.forEach(one::putIfAbsent);
        return one;
    }
}