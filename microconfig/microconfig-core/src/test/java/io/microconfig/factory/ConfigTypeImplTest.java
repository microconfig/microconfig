package io.microconfig.factory;

import org.junit.jupiter.api.Test;

import static io.microconfig.factory.ConfigTypeImpl.byNameAndTypes;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigTypeImplTest {
    @Test
    void testByNameAndTypes() {
        ConfigType app = byNameAndTypes("app", "application", singleton(".yaml"));
        assertEquals("app", app.getName());
        assertEquals("application", app.getResultFileName());
        assertEquals(singleton(".yaml"), app.getConfigExtensions());
    }

    @Test
    void testExtension() {
        assertThrows(IllegalArgumentException.class, () -> byNameAndTypes("app", "application", singleton("yaml")));
    }

    @Test
    void testByName() {
        ConfigType app = ConfigTypeImpl.byName("app");
        assertEquals("app", app.getName());
        assertEquals("app", app.getResultFileName());
        assertEquals(singleton(".app"), app.getConfigExtensions());
    }
}