package io.microconfig.factory.configtypes;

import io.microconfig.factory.ConfigType;
import org.junit.jupiter.api.Test;

import static io.microconfig.factory.configtypes.ConfigTypeImpl.byNameAndExtensions;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypeImplTest {
    @Test
    void testByNameAndTypes() {
        ConfigType app = byNameAndExtensions("app", singleton(".yaml"), "application");
        assertEquals("app", app.getType());
        assertEquals("application", app.getResultFileName());
        assertEquals(singleton(".yaml"), app.getSourceExtensions());
    }

    @Test
    void testByName() {
        ConfigType app = ConfigTypeImpl.byName("app");
        assertEquals("app", app.getType());
        assertEquals("app", app.getResultFileName());
        assertEquals(singleton(".app"), app.getSourceExtensions());
    }

    @Test
    void testExtension() {
        assertThrows(IllegalArgumentException.class, () -> byNameAndExtensions("app", singleton("yaml"), "application"));
    }
}