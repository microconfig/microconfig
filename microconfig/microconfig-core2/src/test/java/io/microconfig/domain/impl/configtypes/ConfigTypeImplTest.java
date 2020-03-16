package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.microconfig.domain.impl.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static io.microconfig.domain.impl.configtypes.ConfigTypeImpl.byName;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;

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
        Set<String> badExtension = singleton("yaml");
        assertThrows(IllegalArgumentException.class, () -> byNameAndExtensions("app", badExtension, "application"));
    }
}