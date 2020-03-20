package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import io.microconfig.core.configtypes.ConfigTypeRepository;

import java.util.List;

import static java.util.Arrays.asList;

public class StandardConfigTypeRepository implements ConfigTypeRepository {
    private final List<ConfigType> standardTypes = asList(StandardConfigType.values());

    @Override
    public List<ConfigType> getConfigTypes() {
        return standardTypes;
    }
}