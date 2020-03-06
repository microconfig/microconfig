package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentDoesNotExistException;
import io.microconfig.core.environments.EnvironmentProvider;
import org.junit.jupiter.api.Test;

import static io.microconfig.testutils.MicronconfigTestFactory.getEnvProvider;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class JsonEnvironmentProviderTest {
    private final EnvironmentProvider environmentProvider = getEnvProvider();

    @Test
    void testGetEnv() {
        Environment uat = environmentProvider.getByName("uat");
        assertEquals("uat", uat.getName());
        assertEquals(7, uat.getComponentGroups().size());

        ComponentGroup componentGroup = uat.getComponentGroups().get(0);
        assertEquals("fnd1", componentGroup.getName());
        assertEquals("172.30.162.3", componentGroup.getIp().get());
        assertEquals(asList("th-server", "th-cache-node1", "th-cache-node2", "th-client"), componentGroup.getComponentNames());
    }

    @Test
    void testIncludes() {
        Environment demo = environmentProvider.getByName("demo");
        assertEquals("100.10.20.1", demo.getIp().get());
        assertEquals("2.2.2.2", demo.getGroupByName("c3").getIp().get());
        assertEquals(singletonList("th-server"), demo.getGroupByName("fnd1").getComponentNames());
        assertEquals(demo.getComponentGroups().size() - 1, demo.getGroupByIp(demo.getIp().get()).size());
        assertEquals(asList("th-cache-node3", "th-cache-node4", "th-cache-proxy"), demo.getGroupByName("fnd2").getComponentNames());
    }

    @Test
    void testIncludesOverrides() {
        Environment env = environmentProvider.getByName("test-env-include");

        assertFalse(env.getIp().isPresent());
        // overridden ip
        assertEquals("172.30.162.8", env.getGroupByName("fnd1").getIp().get());
        // overridden ip and components
        assertEquals("172.30.162.9", env.getGroupByName("fnd2").getIp().get());
        assertEquals(singletonList("th-cache-node7"), env.getGroupByName("fnd2").getComponentNames());
        // overridden components/original ip
        assertEquals("1.1.1.1", env.getGroupByName("c3").getIp().get());
        assertEquals(asList("override1", "override2"), env.getGroupByName("c3").getComponentNames());
        // original components/original ip
        assertEquals("2.2.2.2", env.getGroupByName("override1").getIp().get());
        assertEquals(singletonList("tov1"), env.getGroupByName("override1").getComponentNames());
    }

    @Test
    void testIncludeAbstractEnvAndIpOverrides() {
        Environment env = environmentProvider.getByName("test-include-abstract-env");

        assertFalse(env.getIp().isPresent());
        assertEquals("172.30.162.8", env.getGroupByName("fnd1").getIp().get());
        assertEquals("172.30.162.9", env.getGroupByName("fnd2").getIp().get());
    }

    @Test
    void testExcludeComponentFromOverriddenEnv() {
        Environment env = environmentProvider.getByName("test-component-exclude2");
        assertEquals(asList("th-server", "th-client"), env.getGroupByName("fnd1").getComponentNames());
    }

    @Test
    void testThrowsExceptionInCaseOfDuplicateComponents() {
        try {
            environmentProvider.getByName("duplicate-components");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Env [duplicate-components] contains several definitions of [th-cache-node7] component", ex.getMessage());
        }
    }

    @Test
    void testExclude() {
        Environment demo = environmentProvider.getByName("demo");
        assertThrows(IllegalArgumentException.class, () -> demo.getGroupByName("mc"));
    }

    @Test
    void testGetIncorrectEnv() {
        assertThrows(EnvironmentDoesNotExistException.class, () -> environmentProvider.getByName("not-exists"));
    }
}