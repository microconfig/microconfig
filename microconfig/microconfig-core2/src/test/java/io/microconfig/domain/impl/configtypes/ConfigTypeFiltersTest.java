package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.configTypeWithName;
import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.domain.impl.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.domain.impl.configtypes.StandardConfigType.HELM;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigTypeFiltersTest {
    private final List<ConfigType> types = new StandardConfigTypeRepository().getConfigTypes();

    @Test
    void selectEachConfigType() {
        List<ConfigType> selected = eachConfigType().selectTypes(types);
        assertEquals(types, selected);
    }

    @Test
    void selectByConfigName() {
        List<ConfigType> selected = configTypeWithName("app", "helm").selectTypes(types);
        assertEquals(asList(APPLICATION, HELM), selected);
    }
}