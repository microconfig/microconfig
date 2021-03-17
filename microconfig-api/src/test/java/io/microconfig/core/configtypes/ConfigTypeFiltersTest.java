package io.microconfig.core.configtypes;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.core.configtypes.ConfigTypeFilters.*;
import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigTypeFiltersTest {
    ConfigType app = type("app");
    ConfigType process = type("process");
    ConfigType deploy = type("deploy");
    List<ConfigType> supportedTypes = asList(app, process, deploy);

    @Test
    void selectEachConfigType() {
        assertEquals(
                supportedTypes,
                eachConfigType().selectTypes(supportedTypes)
        );
    }

    @Test
    void useProvidedTypes() {
        ConfigType testType = type("provided");
        assertEquals(
                singletonList(testType),
                configType(testType).selectTypes(supportedTypes)
        );
    }

    @Test
    void selectByConfigName() {
        assertEquals(
                asList(app, deploy),
                configTypeWithName("app", "deploy").selectTypes(supportedTypes)
        );
    }

    @Test
    void failOnUnsupportedName() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName("app", "badName").selectTypes(supportedTypes));
    }

    @Test
    void failOnEmptyNames() {
        assertThrows(IllegalArgumentException.class, () -> configTypeWithName().selectTypes(supportedTypes));
        assertThrows(IllegalArgumentException.class, () -> configType().selectTypes(supportedTypes));
    }

    @Test
    void selectByFileExtension() {
        assertEquals(
                singletonList(app),
                configTypeWithExtensionOf(new File("application.app")).selectTypes(supportedTypes)
        );
    }

    @Test
    void failOnFileWithoutExtension() {
        File badFile = new File("application");
        assertThrows(IllegalArgumentException.class, () -> configTypeWithExtensionOf(badFile).selectTypes(supportedTypes));
    }

    @Test
    void selectByResultFileExtension(){
        assertEquals(
                asList(app),
                configTypeWithResultFileExtension(".app").selectTypes(supportedTypes)
        );
        assertEquals(
                supportedTypes,
                configTypeWithResultFileExtension(null).selectTypes(supportedTypes)
        );
    }

    @Test
    void failOnInvalidResultFileExtensions(){
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension("nodot"));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(""));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(" "));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension("."));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(". "));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(".."));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(" .almostgood"));
        assertThrows(IllegalArgumentException.class, () -> configTypeWithResultFileExtension(".so close"));
    }

    private ConfigType type(String name) {
        ConfigType ct = mock(ConfigType.class);
        when(ct.getName()).thenReturn(name);
        when(ct.getResultFileName()).thenReturn("file-" + name);
        when(ct.getSourceExtensions()).thenReturn(setOf("." + name));
        when(ct.getResultFileExtension()).thenReturn("." + name);
        return ct;
    }
}