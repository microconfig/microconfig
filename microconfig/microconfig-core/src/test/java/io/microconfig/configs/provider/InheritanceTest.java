package io.microconfig.configs.provider;

import io.microconfig.environments.Environment;
import io.microconfig.environments.EnvironmentProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.microconfig.utils.MicronconfigTestFactory.getEnvProvider;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InheritanceTest {
    private final EnvironmentProvider environmentProvider = getEnvProvider();

    @Test
    void testIncludes() {
        goTest("e1", "s1", "s2");
        goTest("e2", "s1", "s3");
        goTest("e3", "s3", "s4");
        goTest("e4", "s4", "s5");
    }

    private void goTest(String env, String... services) {
        Environment demo = environmentProvider.getByName(env);
        List<String> expected = asList(services);
        List<String> result = demo.getGroupByName("g1").getComponentNames();
        assertEquals(expected, result);
    }
}