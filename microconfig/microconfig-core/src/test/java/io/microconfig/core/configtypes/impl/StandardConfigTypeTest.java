package io.microconfig.core.configtypes.impl;

import io.microconfig.core.configtypes.ConfigType;
import org.junit.jupiter.api.Test;

import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.utils.CollectionUtils.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConfigTypeTest {
    @Test
    public void shouldImplementConfigType() {
        ConfigType app = APPLICATION;
        assertEquals("app", app.getName());
        assertEquals(setOf(".yaml", ".properties"), app.getSourceExtensions());
        assertEquals("application", app.getResultFileName());
    }
}