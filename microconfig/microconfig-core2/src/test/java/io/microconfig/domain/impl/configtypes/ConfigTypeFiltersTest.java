package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.*;
import static io.microconfig.domain.impl.configtypes.StandardConfigType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypeFiltersTest {
    private final List<ConfigType> types = new StandardConfigTypeRepository().getConfigTypes();

    @Test
    void selectEachConfigType() {
        List<ConfigType> selected = eachConfigType().selectTypes(types);
        assertEquals(types, selected);
    }

    @Test
    void useProvidedTypes() {
        List<ConfigType> selected = configType(DEPLOY).selectTypes(emptyList());
        assertEquals(singletonList(DEPLOY), selected);
    }

    @Test
    void selectByConfigName() {
        List<ConfigType> selected = configTypeWithName("app", "helm").selectTypes(types);
        assertEquals(asList(APPLICATION, HELM), selected);
    }

    @Test
    void failOnUnsupportedName() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName("badName").selectTypes(types));
    }

    @Test
    void selectByFileExtension() {
        File application = new File("application.yaml");
        List<ConfigType> selected = configTypeWithExtensionOf(application).selectTypes(types);
        assertEquals(singletonList(APPLICATION), selected);
    }

    @Test
    void failOnFileWithoutExtension() {
        File badFile = new File("application");
        assertThrows(IllegalArgumentException.class, () -> configTypeWithExtensionOf(badFile).selectTypes(types));
    }

}