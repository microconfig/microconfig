package io.microconfig.integration;

import io.microconfig.Microconfig;
import io.microconfig.domain.CompositeComponentProperties;
import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import static io.microconfig.ClasspathUtils.classpathFile;
import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.utils.CollectionUtils.setOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroconfigTest {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));

    @Test
    void ip() {
        String value = build("uat", "ip1")
                .getPropertyWithKey("ip1.some-ip")
                .map(Property::getValue)
                .orElseThrow(IllegalStateException::new);

        assertEquals("1.1.1.1", value);
    }

    @Test
    void simpleInclude() {
        assertEquals(setOf("i1.prop", "i2.prop", "i3.prop"), build("uat", "i1").getPropertiesAsMap().keySet());
    }

    private CompositeComponentProperties build(String env, String component) {
        return microconfig.inEnvironment(env)
                .findComponentWithName(component, false)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver());
    }
}
