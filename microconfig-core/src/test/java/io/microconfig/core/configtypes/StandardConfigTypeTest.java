package io.microconfig.core.configtypes;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.utils.CollectionUtils.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StandardConfigTypeTest {
    @Test
    public void shouldImplementConfigType() {
        ConfigType app = APPLICATION;
        assertEquals("app", app.getName());
        assertEquals(setOf(".yml", ".yaml", ".properties"), app.getSourceExtensions());
        assertEquals("application", app.getResultFileName());
        assertNull(app.getResultFileExtension());
    }
}