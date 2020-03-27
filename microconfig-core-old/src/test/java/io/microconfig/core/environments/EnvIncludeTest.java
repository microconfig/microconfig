package io.microconfig.core.environments;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.microconfig.core.environments.Component.byType;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Optional.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class EnvIncludeTest {
    private Component zuul = byType("zuul");
    private Component eureka = byType("eureka");
    private Component monitoring = byType("monitoring");

    @Test
    void includeTo() {
        Environment base = baseEnv();
        Environment dev = devEnv("3.4.5.6");

        EnvironmentProvider provider = Mockito.mock(EnvironmentProvider.class);
        when(provider.getByName("base")).thenReturn(base);

        Environment processed = dev.processInclude(provider);
        assertEquals(asList(zuul, monitoring), processed.getComponentsByGroup("infra"));
        assertNotNull(processed.getGroupByName("newGroup"));
        assertEquals(of("3.4.5.6"), processed.getGroupByName("ipGroup").getIp());

        Environment processed2 = devEnv(null).processInclude(provider);
        assertEquals(of("1.1.1.2"), processed2.getGroupByName("ipGroup").getIp());

    }

    private Environment baseEnv() {
        ComponentGroup infra = new ComponentGroup(
                "infra",
                of("1.2.3.4"),
                asList(zuul, eureka),
                emptyList(),
                emptyList()
        );
        ComponentGroup ipGroup = new ComponentGroup("ipGroup", empty(), emptyList(), emptyList(), emptyList());

        return new Environment(
                "base",
                asList(infra, ipGroup),
                of("1.1.1.2"),
                empty(),
                empty(),
                ""
        );
    }

    private Environment devEnv(String ip) {
        ComponentGroup infra = new ComponentGroup(
                "infra",
                of("1.2.3.4"),
                emptyList(),
                singletonList(eureka),
                singletonList(monitoring)
        );
        ComponentGroup newGroup = new ComponentGroup("newGroup", empty(), emptyList(), emptyList(), emptyList());

        return new Environment(
                "dev",
                asList(infra, newGroup),
                ofNullable(ip),
                of(1),
                of(new EnvInclude("base", emptySet())),
                ""
        );
    }

}