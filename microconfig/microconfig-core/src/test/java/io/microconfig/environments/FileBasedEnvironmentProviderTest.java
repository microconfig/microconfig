package io.microconfig.environments;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static io.microconfig.utils.MicronconfigTestFactory.getEnvironmentProvider;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

class FileBasedEnvironmentProviderTest {
    private final EnvironmentProvider environmentProvider = getEnvironmentProvider();

    @Test
    void testEnvName() {
        Set<String> environmentNames = environmentProvider.getEnvironmentNames();
        assertEquals(new HashSet<>(asList("test-component-exclude1", "p1", "test-component-exclude2", "test-env-include-err",
                "aliases", "base-env", "var", "uat", "demo", "dev2", "dev", "test-env-include", "test-include-abstract-env", "duplicate-components", "e1", "e2", "e3", "e4")),
                environmentNames);
    }

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

        assertTrue(!env.getIp().isPresent());
        // overriden ip
        assertEquals("172.30.162.8", env.getGroupByName("fnd1").getIp().get());
        // overriden ip and components
        assertEquals("172.30.162.9", env.getGroupByName("fnd2").getIp().get());
        assertEquals(singletonList("th-cache-node7"), env.getGroupByName("fnd2").getComponentNames());
        // overriden components/original ip
        assertEquals("1.1.1.1", env.getGroupByName("c3").getIp().get());
        assertEquals(asList("override1", "override2"), env.getGroupByName("c3").getComponentNames());
        // original components/original ip
        assertEquals("2.2.2.2", env.getGroupByName("override1").getIp().get());
        assertEquals(singletonList("tov1"), env.getGroupByName("override1").getComponentNames());// overriden components
    }

    @Test
    void testIncludeAbstractEnvAndIpOverrides() {
        Environment env = environmentProvider.getByName("test-include-abstract-env");

        assertTrue(!env.getIp().isPresent());
        assertEquals("172.30.162.8", env.getGroupByName("fnd1").getIp().get());
        assertEquals("172.30.162.9", env.getGroupByName("fnd2").getIp().get());
    }

    @Test
    void testExcludeComponentFromOverriddenEnv() {
        Environment env = environmentProvider.getByName("test-component-exclude2");
        assertEquals(asList("th-server", "th-client"), env.getGroupByName("fnd1").getComponentNames());
    }

    @Test
    void testThrowsExceptionInCaseOfMissingIP() {
        Environment env = environmentProvider.getByName("test-env-include-err");
        assertThrows(IllegalArgumentException.class, env::verifyIpsSet);
    }

    @Test
    void testThrowsExceptionInCaseOfDuplicateComponents() {
        try {
            environmentProvider.getByName("duplicate-components");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Env [duplicate-components] containsInnerFile several definitions of [th-cache-node7] component", ex.getMessage());
        }
    }

    @Test
    void testExclude() {
        Environment demo = environmentProvider.getByName("demo");
        assertThrows(IllegalArgumentException.class, () -> demo.getGroupByName("mc"));
    }

    void testGetIncorrectEnv() {
        assertThrows(IllegalArgumentException.class, () -> environmentProvider.getByName("not-exists"));
    }
}
