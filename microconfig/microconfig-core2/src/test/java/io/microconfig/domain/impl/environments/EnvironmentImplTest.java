package io.microconfig.domain.impl.environments;

import io.microconfig.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnvironmentImplTest {
    Component one = mock(Component.class);
    Component two = mock(Component.class);
    Component three = mock(Component.class);
    Components components1 = mock(Components.class);
    Components components2 = mock(Components.class);
    ComponentGroup group1 = mock(ComponentGroup.class);
    ComponentGroup group2 = mock(ComponentGroup.class);

    List<ComponentGroup> groups = asList(group1, group2);
    ComponentFactory factory = mock(ComponentFactory.class);

    Environment env = new EnvironmentImpl("dev", groups, factory);

    @BeforeEach
    void setUp() {
        when(group1.getComponents()).thenReturn(components1);
        when(group2.getComponents()).thenReturn(components2);
        when(components1.asList()).thenReturn(asList(one, two));
        when(components2.asList()).thenReturn(singletonList(three));
    }

    @Test
    void filterGroupsByIp() {
        String ip = "127.0.0.1";
        when(group2.getIp()).thenReturn(of(ip));

        assertEquals(singletonList(group2), env.findGroupsWithIp(ip));
    }

    @Test
    void findGroupByName() {
        when(group1.getName()).thenReturn("group1");
        when(group2.getName()).thenReturn("group2");

        assertEquals(group2, env.findGroupWithName("group2"));
        assertThrows(IllegalArgumentException.class, () -> env.findGroupWithName("group3"));
    }

    @Test
    void findGroupWithComponentName() {
        when(group2.findComponentWithName("three")).thenReturn(of(three));

        assertEquals(group2, env.findGroupWithComponent("three"));
        assertThrows(IllegalArgumentException.class, () -> env.findGroupWithComponent("not found"));
    }

    @Test
    void getAllComponents() {
        List<Component> allComponents = asList(one, two, three);

        Components expected = mock(Components.class);
        when(factory.toComponents(allComponents)).thenReturn(expected);

        assertSame(expected, env.getAllComponents());
    }

    @Test
    void getComponentWithName() {
        when(group1.findComponentWithName("two")).thenReturn(of(two));
        assertEquals(two, env.findComponentWithName("two", true));

        assertThrows(IllegalArgumentException.class, () -> env.findComponentWithName("four", true));
    }

    @Test
    void findComponentWithName() {
        when(group1.findComponentWithName("two")).thenReturn(of(two));
        assertEquals(two, env.findComponentWithName("two", false));

        Component four = mock(Component.class);
        when(factory.createComponent("four", "four", env.getName())).thenReturn(four);
        assertEquals(four, env.findComponentWithName("four", false));
    }
}