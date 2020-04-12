package io.microconfig.core.configtypes;

import java.util.List;

import static java.util.Arrays.asList;

public class StandardConfigTypeRepository implements ConfigTypeRepository {
    private final List<ConfigType> standardTypes = asList(StandardConfigType.values());

    @Override
    public List<ConfigType> getConfigTypes() {
        return standardTypes;
    }
}