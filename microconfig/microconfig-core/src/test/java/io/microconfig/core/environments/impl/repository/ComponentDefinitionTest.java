package io.microconfig.core.environments.impl.repository;

import io.microconfig.core.environments.Component;
import io.microconfig.core.environments.impl.ComponentFactory;
import org.junit.jupiter.api.Test;

import static io.microconfig.core.environments.impl.repository.ComponentDefinition.withAlias;
import static io.microconfig.core.environments.impl.repository.ComponentDefinition.withName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentDefinitionTest {
    String alias = "alias";
    String name = "name";

    @Test
    void factoryMethods() {
        assertEquals(new ComponentDefinition(alias, name), withAlias(alias, name));
        assertEquals(new ComponentDefinition(name, name), withName(name));
    }

    @Test
    void toComponent() {
        String env = "dev";
        ComponentFactory factory = mock(ComponentFactory.class);
        Component expected = mock(Component.class);
        when(factory.createComponent(alias, name, env)).thenReturn(expected);
        assertSame(expected, withAlias(alias, name).toComponent(factory, env));
    }
}