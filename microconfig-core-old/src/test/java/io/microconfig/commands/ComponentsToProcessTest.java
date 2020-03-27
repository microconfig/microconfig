package io.microconfig.commands;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.ComponentGroup;
import io.microconfig.core.environments.Environment;
import io.microconfig.core.environments.EnvironmentProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.microconfig.core.environments.Component.byType;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ComponentsToProcessTest {
    private final String group = "infra";
    private final String envName = "prod";
    private final Component zuul = byType("zuul");
    private final Component eureka = byType("eureka");
    private final List<Component> components = asList(zuul, eureka);
    private final List<ComponentGroup> groups = singletonList(
            new ComponentGroup(group, empty(), components, emptyList(), emptyList())
    );

    @Test
    void textComponents() {
        EnvironmentProvider provider = mockProvider();

        ComponentsToProcess groupContext = new ComponentsToProcess(envName, group, emptyList());
        assertEquals(components, groupContext.components(provider));

        ComponentsToProcess componentContext = new ComponentsToProcess(envName, singletonList("zuul"));
        assertEquals(singletonList(zuul), componentContext.components(provider));

        ComponentsToProcess fullContext = new ComponentsToProcess(envName, group, singletonList("eureka"));
        assertEquals(singletonList(eureka), fullContext.components(provider));

        ComponentsToProcess fullContext2 = new ComponentsToProcess(envName, group, singletonList("missingService"));
        assertThrows(IllegalArgumentException.class, () -> fullContext2.components(provider));
    }

    @Test
    void testEnv() {
        assertEquals(envName, new ComponentsToProcess(envName, emptyList()).env());
    }

    private EnvironmentProvider mockProvider() {
        EnvironmentProvider provider = Mockito.mock(EnvironmentProvider.class);
        Environment env = new Environment(envName, groups, empty(), empty(), empty(), "");
        when(provider.getByName(envName)).thenReturn(env);
        return provider;
    }

}