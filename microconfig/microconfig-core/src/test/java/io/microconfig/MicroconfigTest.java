package io.microconfig;

import io.microconfig.core.properties.Properties;
import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.impl.PropertyResolveException;
import org.junit.jupiter.api.Test;

import static io.microconfig.Microconfig.searchConfigsIn;
import static io.microconfig.core.configtypes.impl.ConfigTypeFilters.configType;
import static io.microconfig.core.configtypes.impl.StandardConfigType.APPLICATION;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.StringUtils.splitKeyValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MicroconfigTest {
    private final Microconfig microconfig = searchConfigsIn(classpathFile("repo"));

    @Test
    void aliasesAndThis() {
        //todo
        testAliases("node1", "app.ip=172.30.162.4", "app.name=node1", "app.value=v1");
//        testAliases("node3", "app.ip=172.30.162.5", "app.name=node3", "app.value=v1");
//        testAliases("node", "app.ip=172.30.162.5", "app.name=node", "app.value=v1");
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

    @Test
    void predefinedFunction() {
        assertEquals(
                splitKeyValue("notFound=", "xmx=0m", "xmxLine=Xmx100m"),
                buildComponent("predefinedFunctions", "uat").propertiesAsKeyValue()
        );
    }

    @Test
    void placeholderToSpel() {
        assertEquals(
                splitKeyValue("test.mq.address=tcp://:6872", "test.mq.address2=tcp://:68720"),
                buildComponent("pts", "dev").propertiesAsKeyValue()
        );
    }

    @Test
    void thisToVar() {
        assertEquals(
                splitKeyValue("c=3"),
                buildComponent("var", "dev").propertiesAsKeyValue()
        );
    }

    @Test
    void testCyclicDetect() {
        assertThrows(PropertyResolveException.class, () -> buildComponent("cyclicDetect", "uat"));
    }

    private void testAliases(String component, String... keyValue) {
        assertEquals(
                splitKeyValue(keyValue),
                buildComponent(component, "aliases").propertiesAsKeyValue()
        );
    }

    private Properties buildComponent(String component, String env) {
        return microconfig.inEnvironment(env)
                .findComponentWithName(component, false)
                .getPropertiesFor(configType(APPLICATION))
                .resolveBy(microconfig.resolver())
                .withoutTempValues();
    }
}
