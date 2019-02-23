package io.microconfig.environments.filebased;

import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;
import io.microconfig.environments.filebased.parsers.YamlEnvironmentParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.microconfig.utils.ClasspathUtils.read;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class YamlEnvironmentParserTest {
    private final YamlEnvironmentParser parser = new YamlEnvironmentParser();

    @Test
    void parse() {
        String name = "realYaml";
        Environment environment = parser.parse(name, read("test-props/envs/yaml/baseYaml.yaml"));
        assertEquals(name, environment.getName());
        assertEquals("1.2.3.4", environment.getIp().get());
        assertEquals(1, (int) environment.getPortOffset().get());
        assertFalse(environment.getInclude().isPresent());
        List<ComponentGroup> expectedGroups = expectedGroups();
        compareGroups(expectedGroups, environment.getComponentGroups());
    }

    private void compareGroups(List<ComponentGroup> expectedGroups, List<ComponentGroup> componentGroups) {
        assertEquals(expectedGroups.size(), componentGroups.size());
        for (int i = 0; i < expectedGroups.size(); i++) {
            ComponentGroup g1 = expectedGroups.get(i);
            ComponentGroup g2 = componentGroups.get(i);
            assertEquals(g1.getIp(), g2.getIp());
            assertEquals(g1.getName(), g2.getName());
            assertEquals(g1.getExcludedComponents(), g2.getExcludedComponents());
            assertEquals(g1.getAppendedComponents(), g2.getAppendedComponents());
        }
    }

    private List<ComponentGroup> expectedGroups() {
        Optional<String> ip = of("1.2.3.4");
        return asList(
                new ComponentGroup("orders", ip, components("order-db-patcher", "order-service", "order-ui", "recommendations"), emptyList(), emptyList()),
                new ComponentGroup("payments", ip, components("payment-db-patcher", "payment-service", "payment-ui"), emptyList(), emptyList()),
                new ComponentGroup("infra", of("17.44.48.45"), components("service-discovery", "api-gateway"), emptyList(), emptyList()),
                new ComponentGroup("kafka", ip, components("zookeeper", "kafka"), emptyList(), emptyList())
        );
    }

    private List<Component> components(String... names) {
        return Stream.of(names)
                .map(Component::byType)
                .collect(toList());
    }
}