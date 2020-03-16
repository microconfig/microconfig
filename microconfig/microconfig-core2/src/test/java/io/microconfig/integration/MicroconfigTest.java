package io.microconfig.integration;

import io.microconfig.Microconfig;
import io.microconfig.domain.CompositeComponentProperties;
import io.microconfig.domain.Property;
import org.junit.jupiter.api.Test;

import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.domain.impl.configtypes.ConfigTypeFilters.eachConfigType;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.StreamUtils.splitKeyValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroconfigTest {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));

    @Test
    void aliasesAndThis() {
        //todo
//        testAliases("node1", "app.ip2=172.30.162.4", "app.name=node1");
//        testAliases("node3", "app.ip2=172.30.162.5", "app.name=node3");
//        testAliases("node", "app.ip2=172.30.162.5", "app.name=node");
    }

    @Test
    void placeholderToAliases() {
        assertEquals(
                splitKeyValue("ips=172.30.162.4 172.30.162.5 172.30.162.5", "properties=node1 node3 node"),
                buildComponent("placeholderToAlias", "aliases").propertiesAsKeyValue()
        );
    }

    @Test
    void ip() {
        String value = buildComponent("ip1", "uat")
                .getPropertyWithKey("ip1.some-ip")
                .map(Property::getValue)
                .orElseThrow(IllegalStateException::new);

        assertEquals("1.1.1.1", value);
    }

    @Test
    void simpleInclude() {
        assertEquals(
                splitKeyValue("key1=1", "key2=2", "key3=3", "key4=4"),
                buildComponent("si1", "uat").propertiesAsKeyValue()
        );
    }

    @Test
    void cyclicInclude() {
        assertEquals(
                splitKeyValue("key1=1", "key2=2", "key3=3"),
                buildComponent("ci1", "uat").propertiesAsKeyValue()
        );
    }

    private void testAliases(String component, String... keyValue) {
        assertEquals(
                splitKeyValue(keyValue),
                buildComponent(component, "aliases").propertiesAsKeyValue()
        );
    }

    private CompositeComponentProperties buildComponent(String component, String env) {
        return microconfig.inEnvironment(env)
                .findComponentWithName(component, false)
                .getPropertiesFor(eachConfigType())
                .resolveBy(microconfig.resolver());
    }
}
