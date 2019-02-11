package io.microconfig.configs.properties.files.parser;

import io.microconfig.configs.environment.Environment;
import io.microconfig.configs.environment.EnvironmentProvider;
import org.junit.Test;

import java.util.List;

import static io.microconfig.configs.utils.EnvFactory.newEnvironmentProvider;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class InheritanceTest {
    private final EnvironmentProvider environmentProvider = newEnvironmentProvider();

    @Test
    public void testIncludes() {
        goTest("e1", "s1", "s2");
        goTest("e2", "s1", "s3");
        goTest("e3", "s3", "s4");
        goTest("e4", "s4", "s5");
    }

    private void goTest(String env, String... services) {
        Environment demo = environmentProvider.getByName(env);
        List<String> expected = asList(services);
        List<String> result = demo.getComponentGroupByName("g1").getComponentNames();
        assertEquals(expected, result);
    }
}