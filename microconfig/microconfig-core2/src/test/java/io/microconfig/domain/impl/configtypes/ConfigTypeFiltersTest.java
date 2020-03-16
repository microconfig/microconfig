package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTypeFiltersTest {
    private final List<ConfigType> types = new StandardConfigTypeRepository().getConfigTypes();

    @Test
    void selectEachConfigType() {
        List<ConfigType> filteredTypes = eachConfigType().selectTypes(types);
        assertEquals(types, filteredTypes);
    }
}