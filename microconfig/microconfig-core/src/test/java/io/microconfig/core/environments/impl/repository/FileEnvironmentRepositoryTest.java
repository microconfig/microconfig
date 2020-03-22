package io.microconfig.core.environments.impl.repository;

import io.microconfig.core.configtypes.ConfigTypeRepository;
import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.impl.ComponentFactoryImpl;
import io.microconfig.core.properties.PropertiesFactory;
import io.microconfig.io.DumpedFsReader;
import io.microconfig.io.FsReader;
import io.microconfig.utils.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.List;

import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileEnvironmentRepositoryTest {
    File dir = classpathFile("envsTest/good");
    ConfigTypeRepository configType = mock(ConfigTypeRepository.class);
    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
    FsReader fsReader = new DumpedFsReader();
    ComponentFactoryImpl componentFactory = new ComponentFactoryImpl(configType, propertiesFactory);
    FileEnvironmentRepository repo = new FileEnvironmentRepository(dir, fsReader, componentFactory);

    @Test
    void noEnvDirException() {
        assertThrows(IllegalArgumentException.class,
                () -> new FileEnvironmentRepository(classpathFile("configTypes"), fsReader, componentFactory));
    }

    @Test
    void envExceptionWrappingOnFileRead() {
        FsReader badReader = Mockito.mock(FsReader.class);
        when(badReader.readFully(any(File.class))).thenThrow(new IllegalArgumentException());
        FileEnvironmentRepository repo = new FileEnvironmentRepository(dir, badReader, componentFactory);
        assertThrows(EnvironmentException.class, () -> repo.environments());
    }

    @Test
    void envExceptionWrappingOnGroupParse() {
        File badDir = classpathFile("envsTest/bad");
        FileEnvironmentRepository repo = new FileEnvironmentRepository(badDir, fsReader, componentFactory);
        assertThrows(EnvironmentException.class, () -> repo.getByName("badGroup"));
    }

    @Test
    void sameEnvNamesException() {
        File badDir = classpathFile("envsTest/bad");
        FileEnvironmentRepository repo = new FileEnvironmentRepository(badDir, fsReader, componentFactory);

        assertThrows(EnvironmentException.class, () -> repo.getByName("bad"));
        assertThrows(EnvironmentException.class, () -> repo.getByName("bad2"));
        assertThrows(IllegalStateException.class, () -> repo.getByName("copy"));
    }

    @Test
    void parseEnvFiles() {
        List<Environment> environments = repo.environments();
        assertEquals(5, environments.size());

        testDev(filter(environments, "dev"));
        testTest(filter(environments, "test"));
        testStaging(filter(environments, "staging"));
        testProd(filter(environments, "prod"));
        testAlias(filter(environments, "alias"));
    }

    @Test
    void filterMethods() {
        assertEquals(CollectionUtils.setOf("alias", "dev", "test", "staging", "prod"), repo.environmentNames());

        assertEquals("dev", repo.getByName("dev").getName());
        assertThrows(EnvironmentException.class, () -> repo.getByName("bad"));

        Environment dev = repo.getOrCreateByName("dev");
        assertEquals("dev", dev.getName());
        assertEquals(11, dev.getAllComponents().asList().size());

        Environment fake = repo.getOrCreateByName("fake");
        assertEquals("fake", fake.getName());
        assertEquals(0, fake.getAllComponents().asList().size());
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
                "api-gateway",
                "selenium"
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

    private void testAlias(Environment env) {
        assertEquals(0, env.getPortOffset());

        testGroup(env, "group", null,
                "componentAlias1",
                "componentAlias2"
        );
    }

    private void testGroup(Environment environment, String groupName, String ip, String... components) {
        ComponentGroup group = environment.findGroupWithName(groupName);
        assertEquals(ofNullable(ip), group.getIp());
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