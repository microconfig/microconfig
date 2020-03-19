package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.microconfig.core.configtypes.impl.ConfigTypeImpl.byName;
import static io.microconfig.core.configtypes.impl.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.utils.CollectionUtils.setOf;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypeImplTest {
    @Test
    void createByNameAndExtension() {
        ConfigType app = byNameAndExtensions("app", singleton(".yaml"), "application");
        assertEquals("app", app.getType());
        assertEquals("application", app.getResultFileName());
        assertEquals(singleton(".yaml"), app.getSourceExtensions());
    }

    @Test
    void createByName() {
        ConfigType app = byName("app");
        assertEquals("app", app.getType());
        assertEquals("app", app.getResultFileName());
        assertEquals(singleton(".app"), app.getSourceExtensions());
    }

    @Test
    void extensionsShouldStartWithDot() {
        Set<String> withBadExtension = setOf(".json", "yaml");
        assertThrows(IllegalArgumentException.class, () -> byNameAndExtensions("app", withBadExtension, "application"));
    }
}