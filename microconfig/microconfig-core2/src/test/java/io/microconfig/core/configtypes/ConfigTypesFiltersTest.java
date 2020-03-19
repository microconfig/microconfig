package io.microconfig.core.configtypes;

import io.microconfig.core.configtypes.impl.ConfigTypeImpl;
import io.microconfig.core.configtypes.impl.StandardConfigTypeRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.core.configtypes.impl.ConfigTypesFilters.*;
import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.core.configtypes.impl.StandardConfigType.HELM;
import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypesFiltersTest {
    private final List<ConfigType> types = new StandardConfigTypeRepository().getConfigTypes();

    @Test
    void selectEachConfigType() {
        assertEquals(
                types,
                eachConfigType().selectTypes(types)
        );
    }

    @Test
    void useProvidedTypes() {
        ConfigType testType = new ConfigTypeImpl("test", setOf(".tst"), "test");
        assertEquals(
                singletonList(testType),
                configType(testType).selectTypes(emptyList())
        );
    }

    @Test
    void selectByConfigName() {
        assertEquals(
                asList(APPLICATION, HELM),
                configTypeWithName("app", "helm").selectTypes(types)
        );
    }

    @Test
    void failOnUnsupportedName() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName("app", "badName").selectTypes(types));
    }

    @Test
    void failOnEmptyNames() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName().selectTypes(types));
        assertThrows(IllegalArgumentException.class, () -> configType().selectTypes(types));
    }

    @Test
    void selectByFileExtension() {
        File application = new File("application.yaml");
        assertEquals(
                singletonList(APPLICATION),
                configTypeWithExtensionOf(application).selectTypes(types)
        );
    }

    @Test
    void failOnFileWithoutExtension() {
        File badFile = new File("application");
        assertThrows(IllegalArgumentException.class, () -> configTypeWithExtensionOf(badFile).selectTypes(types));
    }
}