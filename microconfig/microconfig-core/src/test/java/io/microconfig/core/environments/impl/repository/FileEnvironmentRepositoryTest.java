package io.microconfig.core.environments.impl.repository;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.impl.ComponentFactoryImpl;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FileEnvironmentRepositoryTest {
    File dir = classpathFile("envsTest");
    ConfigTypeRepository configType = mock(ConfigTypeRepository.class);
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
    FileEnvironmentRepository repo = new FileEnvironmentRepository(dir, new DumpedFsReader(), new ComponentFactoryImpl(configType, propertiesFactory));

    @Test
    void parseEnvFiles() {
        List<Environment> environments = repo.environments();
        assertEquals(4, environments.size());
        testDev(filter(environments, "dev"));
        testTest(filter(environments, "test"));
        testStaging(filter(environments, "staging"));
        testProd(filter(environments, "prod"));
    }

    private void testDev(Environment env) {
        assertEquals(0, env.getPortOffset());
        testGroup(env, "orders", "10.0.0.1",
                "order-db-patcher",
                "order-service",
                "order-ui"
        );

        testGroup(env, "payments", "10.0.0.1",
                "payment-db-patcher",
                "payment-service",
                "payment-ui",
                "payment-provider-mock"
        );

        testGroup(env, "infra", "10.0.10.1",
                "service-discovery",
                "api-gateway"
        );

        testGroup(env, "kafka", "10.0.0.1",
                "zookeeper",
                "kafka"
        );
    }

    private void testTest(Environment env) {
        assertEquals(100, env.getPortOffset());
        testGroup(env, "orders", "10.0.0.1",
                "order-db-patcher",
                "order-service",
                "order-ui"
        );

        testGroup(env, "payments", "10.0.0.1",
                "payment-db-patcher",
                "payment-service",
                "payment-ui",
                "payment-provider-mock"
        );

        testGroup(env, "infra", "10.0.10.1",
                "service-discovery",
                "api-gateway"
        );

        testGroup(env, "kafka", "10.0.0.1",
                "zookeeper",
                "kafka"
        );
    }

    private void testStaging(Environment env) {
        assertEquals(0, env.getPortOffset());

        testGroup(env, "orders", "10.20.0.1",
                "order-db-patcher",
                "order-service",
                "order-ui"
        );

        testGroup(env, "payments", "10.20.0.1",
                "payment-db-patcher",
                "payment-service",
                "payment-ui"
        );

        testGroup(env, "infra", "10.20.10.1",
                "service-discovery",
                "api-gateway"
        );

        testGroup(env, "kafka", "10.20.0.1",
                "zookeeper",
                "kafka"
        );

        testGroup(env, "monitoring", "10.20.20.1",
                "grafana",
                "prometheus"
        );
    }

    private void testProd(Environment env) {
        assertEquals(100, env.getPortOffset());

        testGroup(env, "orders", "10.20.0.1",
                "order-db-patcher",
                "order-service",
                "order-ui"
        );

        testGroup(env, "payments", "10.20.0.1",
                "payment-db-patcher",
                "payment-service",
                "payment-ui"
        );

        testGroup(env, "infra", "10.20.10.1",
                "service-discovery",
                "api-gateway"
        );

        testGroup(env, "monitoring", "10.20.20.1",
                "grafana",
                "prometheus"
        );
    }

    private void testGroup(Environment environment, String groupName, String ip, String... components) {
        ComponentGroup group = environment.findGroupWithName(groupName);
        assertEquals(ip, group.getIp().get());
        testComponents(environment.getName(), asList(components), group.getComponents().asList());
    }

    private void testComponents(String env, List<String> expected, List<Component> actual) {
        assertEquals(expected, actual.stream().map(Component::getName).collect(toList()));
        actual.forEach(c -> assertEquals(env, c.getEnvironment()));
    }

    private Environment filter(List<Environment> envs, String name) {
        return envs.stream().filter(e -> e.getName().equals(name)).findFirst().get();
    }

}