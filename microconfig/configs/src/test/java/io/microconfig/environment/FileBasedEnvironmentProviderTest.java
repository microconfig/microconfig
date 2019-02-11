package io.microconfig.environment;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static io.microconfig.utils.EnvFactory.newEnvironmentProvider;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class FileBasedEnvironmentProviderTest {
    private final EnvironmentProvider environmentProvider = newEnvironmentProvider();

    @Test
    public void testEnvName() {
        Set<String> environmentNames = environmentProvider.getEnvironmentNames();
        assertEquals(new HashSet<>(asList("test-component-exclude1", "p1", "test-component-exclude2", "test-env-include-err",
                "aliases", "base-env", "var", "uat", "demo", "dev2", "dev", "test-env-include", "test-include-abstract-env", "duplicate-components", "e1", "e2", "e3", "e4")),
                environmentNames);
    }

    @Test
    public void testGetEnv() {
        Environment uat = environmentProvider.getByName("uat");
        assertEquals("uat", uat.getName());
        assertEquals(7, uat.getComponentGroups().size());

        ComponentGroup componentGroup = uat.getComponentGroups().get(0);
        assertEquals("fnd1", componentGroup.getName());
        assertEquals("172.30.162.3", componentGroup.getIp().orElseThrow());
        assertEquals(asList("th-server", "th-cache-node1", "th-cache-node2", "th-client"), componentGroup.getComponentNames());
    }

    @Test
    public void testIncludes() {
        Environment demo = environmentProvider.getByName("demo");
        assertEquals("100.10.20.1", demo.getIp().orElseThrow());
        assertEquals("2.2.2.2", demo.getComponentGroupByName("c3").getIp().orElseThrow());
        assertEquals(singletonList("th-server"), demo.getComponentGroupByName("fnd1").getComponentNames());
        assertEquals(demo.getComponentGroups().size() - 1, demo.getComponentsGroupByIp(demo.getIp().orElseThrow()).size());
        assertEquals(asList("th-cache-node3", "th-cache-node4", "th-cache-proxy"), demo.getComponentGroupByName("fnd2").getComponentNames());
    }

    @Test
    public void testIncludesOverrides() {
        Environment env = environmentProvider.getByName("test-env-include");

        assertTrue(!env.getIp().isPresent());
        // overriden ip
        assertEquals("172.30.162.8", env.getComponentGroupByName("fnd1").getIp().orElseThrow());
        // overriden ip and components
        assertEquals("172.30.162.9", env.getComponentGroupByName("fnd2").getIp().orElseThrow());
        assertEquals(singletonList("th-cache-node7"), env.getComponentGroupByName("fnd2").getComponentNames());
        // overriden components/original ip
        assertEquals("1.1.1.1", env.getComponentGroupByName("c3").getIp().orElseThrow());
        assertEquals(asList("override1", "override2"), env.getComponentGroupByName("c3").getComponentNames());
        // original components/original ip
        assertEquals("2.2.2.2", env.getComponentGroupByName("override1").getIp().orElseThrow());
        assertEquals(singletonList("tov1"), env.getComponentGroupByName("override1").getComponentNames());// overriden components
    }

    @Test
    public void testIncludeAbstractEnvAndIpOverrides() {
        Environment env = environmentProvider.getByName("test-include-abstract-env");

        assertTrue(!env.getIp().isPresent());
        assertEquals("172.30.162.8", env.getComponentGroupByName("fnd1").getIp().orElseThrow());
        assertEquals("172.30.162.9", env.getComponentGroupByName("fnd2").getIp().orElseThrow());
    }

    @Test
    public void testExcludeComponentFromOverriddenEnv() {
        Environment env = environmentProvider.getByName("test-component-exclude2");
        assertEquals(asList("th-server", "th-client"), env.getComponentGroupByName("fnd1").getComponentNames());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsExceptionInCaseOfMissingIP() {
        Environment env = environmentProvider.getByName("test-env-include-err");
        env.verifyIpsSet();
    }

    @Test
    public void testThrowsExceptionInCaseOfDuplicateComponents() {
        try {
            environmentProvider.getByName("duplicate-components");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("Env [duplicate-components] containsInnerFile several definitions of [th-cache-node7] component", ex.getMessage());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExclude() {
        Environment demo = environmentProvider.getByName("demo");
        demo.getComponentGroupByName("mc");

    }

    @Test(expected = EnvironmentNotExistException.class)
    public void testGetIncorrectEnv() {
        environmentProvider.getByName("not-exists");
    }
}
