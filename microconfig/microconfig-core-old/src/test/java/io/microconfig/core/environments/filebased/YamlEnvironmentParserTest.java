package io.microconfig.core.environments.filebased;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.EnvInclude;
import io.microconfig.core.environments.Environment;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.microconfig.core.environments.Component.byType;
import static io.microconfig.core.environments.filebased.EnvironmentParserImpl.parser;
import static io.microconfig.testutils.ClasspathUtils.read;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class YamlEnvironmentParserTest {
    private final EnvironmentParser parser = parser();
    File file = new File("env.yaml");

    @Test
    void testBase() {
        String name = "baseYaml";
        String ip = "1.2.3.4";
        Environment environment = parser.parse(file, name, read("test-props/envs/yaml/" + name + ".yaml"));
        assertEquals(name, environment.getName());
        assertEquals(ip, environment.getIp().get());
        assertEquals(1, (int) environment.getPortOffset().get());
        assertFalse(environment.getInclude().isPresent());
        compareGroups(baseGroups(of(ip)), environment.getComponentGroups());
    }

    @Test
    void testException() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(file, "dev", ":("));
    }

    @Test
    void testInclude() {
        String name = "includeYaml";
        Environment environment = parser.parse(file, name, read("test-props/envs/yaml/" + name + ".yaml"));
        assertEquals(name, environment.getName());
        String ip = "172.30.40.1";
        assertEquals(ip, environment.getIp().get());
        assertEquals(5, (int) environment.getPortOffset().get());
        compareInclude(environment.getInclude().get());
        compareGroups(withIncludedGroups(of(ip)), environment.getComponentGroups());
    }

    private void compareInclude(EnvInclude envInclude) {
        assertEquals(new EnvInclude("baseYaml", singleton("kafka")), envInclude);
    }

    private void compareGroups(List<ComponentGroup> expected, List<ComponentGroup> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            ComponentGroup g1 = expected.get(i);
            ComponentGroup g2 = actual.get(i);
            assertEquals(g1.getIp(), g2.getIp());
            assertEquals(g1.getName(), g2.getName());
            assertEquals(g1.getExcludedComponents(), g2.getExcludedComponents());
            assertEquals(g1.getAppendedComponents(), g2.getAppendedComponents());
        }
    }

    private List<ComponentGroup> baseGroups(Optional<String> ip) {
        return asList(
                new ComponentGroup("orders", ip, components("order-db-patcher", "order-service", "order-ui", "recommendations"), emptyList(), emptyList()),
                new ComponentGroup("payments", ip, components("payment-db-patcher", "payment-service", "payment-ui"), emptyList(), emptyList()),
                new ComponentGroup("infra", of("17.44.48.45"), components("service-discovery", "api-gateway"), emptyList(), emptyList()),
                new ComponentGroup("kafka", ip, components("zookeeper", "kafka"), emptyList(), emptyList())
        );
    }

    private List<ComponentGroup> withIncludedGroups(Optional<String> ip) {
        return asList(
                new ComponentGroup("infra", ip, emptyList(), singletonList(byType("ssl-api-gateway")), singletonList(byType("local-proxy"))),
                new ComponentGroup("payments", ip, components("payment-service", "payment-ui"), emptyList(), emptyList()),
                new ComponentGroup("cassandra", of("172.30.43.8"), components("cassandra"), emptyList(), emptyList())
        );
    }

    private List<Component> components(String... names) {
        return Stream.of(names)
                .map(Component::byType)
                .collect(toList());
    }
}