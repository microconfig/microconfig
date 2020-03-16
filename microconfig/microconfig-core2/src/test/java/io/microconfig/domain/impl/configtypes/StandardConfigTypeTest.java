package io.microconfig.domain.impl.configtypes;

import io.microconfig.domain.ConfigType;
import org.junit.jupiter.api.Test;

import static io.microconfig.domain.impl.configtypes.StandardConfigType.APPLICATION;
import static io.microconfig.utils.CollectionUtils.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StandardConfigTypeTest {
    @Test
    public void shouldImplementConfigType() {
        ConfigType app = APPLICATION;
        assertEquals("app", app.getType());
        assertEquals(setOf(".yaml", ".properties"), app.getSourceExtensions());
        assertEquals("application", app.getResultFileName());
    }
}