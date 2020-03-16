package io.microconfig.integration;

import io.microconfig.Microconfig;
import io.microconfig.domain.CompositeComponentProperties;
import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.StreamUtils.splitKeyValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroconfigTest {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));

//    @Test
//    void testEnvPropAliases() {
//        doTestAliases("node1", "172.30.162.4");
//        doTestAliases("node2", "172.30.162.4");
//        doTestAliases("node3", "172.30.162.5");
//        doTestAliases("node", "172.30.162.5");
//    }

    @Test
    void placeholderToAliases() {
        Map<String, String> result = build("aliases", "placeholderToAlias").propertiesAsKeyValue();
        assertEquals("172.30.162.4 172.30.162.5", result.get("ips"));
        assertEquals("v1 v1", result.get("properties"));
    }

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
        assertEquals(
                splitKeyValue("key1=1", "key2=2", "key3=3", "key4=4"),
                build("uat", "si1").propertiesAsKeyValue()
        );
    }

    @Test
    void cyclicInclude() {
        assertEquals(
                splitKeyValue("key1=1", "key2=2", "key3=3"),
                build("uat", "ci1").propertiesAsKeyValue()
        );
    }

    private CompositeComponentProperties build(String env, String component) {
        return microconfig.inEnvironment(env)
                .findComponentWithName(component, false)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver());
    }
}
