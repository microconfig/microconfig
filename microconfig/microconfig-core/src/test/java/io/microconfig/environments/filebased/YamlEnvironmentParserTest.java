package io.microconfig.environments.filebased;

import io.microconfig.environments.Component;
import io.microconfig.environments.ComponentGroup;
import io.microconfig.environments.Environment;
import io.microconfig.environments.filebased.parsers.YamlEnvironmentParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static io.microconfig.utils.ClasspathUtils.read;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
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
        assertEquals(expectedGroups(), environment.getComponentGroups());
    }

    private List<ComponentGroup> expectedGroups() {
        return Arrays.asList(
                new ComponentGroup("orders", empty(), components("order-db-patcher", "order-service", "order-ui", "recommendations"), emptyList(), emptyList()),
                new ComponentGroup("payments", empty(), components("payment-db-patcher", "payment-service", "payment-ui"), emptyList(), emptyList()),
                new ComponentGroup("infra", empty(), components("service-discovery", "api-gateway"), emptyList(), emptyList()),
                new ComponentGroup("kafka", empty(), components("zookeeper", "kafka"), emptyList(), emptyList()),
                new ComponentGroup("kafka2", empty(), components("zookeeper2", "kafka2"), emptyList(), emptyList())
        );
    }

    private List<Component> components(String... names) {
        return Stream.of(names)
                .map(Component::byType)
                .collect(toList());
    }
}