package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import io.microconfig.domain.ConfigTypeRepository;

import java.util.List;

import static java.util.Arrays.asList;

public class StandardConfigTypeRepository implements ConfigTypeRepository {

    private static final List<ConfigType> standardTypes = asList(StandardConfigType.values());

    @Override
    public List<ConfigType> getConfigTypes() {
        return standardTypes;
    }
}