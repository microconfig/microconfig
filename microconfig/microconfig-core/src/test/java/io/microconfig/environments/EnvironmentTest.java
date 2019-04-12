package io.microconfig.environments;

import org.junit.jupiter.api.Test;

import static io.microconfig.environments.Component.byType;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentTest {
    private Component eureka = byType("eureka");
    private Component zuul = byType("zuul");
    private Component orderService = byType("order-service");
    private Component orderUi = byType("order-ui");

    @Test
    void testFilterApi() {
        ComponentGroup infraGroup = infraGroup();
        ComponentGroup orderGroup = orderGroup();

        Environment env = prodEnv(infraGroup, orderGroup);

        assertEquals(singletonList(orderGroup), env.getGroupByIp(orderGroup.getIp().get()));
        assertEquals(infraGroup, env.getGroupByName(infraGroup.getName()));
        assertThrows(IllegalArgumentException.class, () -> env.getGroupByName("missingGroup"));
        assertEquals(of(orderGroup), env.getGroupByComponentName(orderUi.getName()));
        assertEquals(orderGroup.getComponents(), env.getComponentsByGroup(orderGroup.getName()));
        assertEquals(asList(eureka, zuul, orderService, orderUi), env.getAllComponents());
        assertEquals(of(orderService), env.getComponentByName(orderService.getName()));
    }

    @Test
    void testSimpleApi() {
        ComponentGroup orderGroup = orderGroup();
        ComponentGroup infraGroup = infraGroup();

        Environment env = prodEnv(orderGroup);
        assertEquals(singletonList(orderGroup), env.getComponentGroups());
        assertTrue(env.getInclude().isPresent());
        env = env.withIncludedGroups(singletonList(infraGroup));
        assertEquals(singletonList(infraGroup), env.getComponentGroups());
        assertEquals(empty(), env.getInclude());

        assertEquals("fileSource", env.getSource());
        String anotherSource = "anotherSource";
        assertEquals(anotherSource, env.withSource(anotherSource).getSource());

        assertEquals("prod", env.getName());
        assertEquals(of("1.1.1.2"), env.getIp());
        assertEquals(of(1), env.getPortOffset());
        assertEquals("prod", env.toString());
    }

    @Test
    void testUniqueNames() {
        Environment unique = prodEnv(orderGroup(), infraGroup());
        assertDoesNotThrow(unique::verifyUniqueComponentNames);

        Environment notUnique = prodEnv(infraGroup(), infraGroup());
        assertThrows(IllegalArgumentException.class, notUnique::verifyUniqueComponentNames);
    }

    private Environment prodEnv(ComponentGroup... groups) {
        return new Environment(
                "prod",
                asList(groups),
                of("1.1.1.2"),
                of(1),
                of(new EnvInclude("base", emptySet())),
                "fileSource"
        );
    }

    private ComponentGroup infraGroup() {
        return new ComponentGroup(
                "infra",
                of("1.2.3.4"),
                asList(eureka, zuul),
                emptyList(),
                emptyList()
        );
    }

    private ComponentGroup orderGroup() {
        return new ComponentGroup(
                "orders",
                of("2.2.3.5"),
                asList(orderService, orderUi),
                emptyList(),
                emptyList()
        );
    }
}