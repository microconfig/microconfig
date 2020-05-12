package io.microconfig.core.environments;

import io.microconfig.core.properties.PropertiesFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    PropertiesFactory factory2 = mock(PropertiesFactory.class);

    Environment env = new EnvironmentImpl(null,"dev", 1, groups, factory, factory2);

    @BeforeEach
    void setUp() {
        when(one.getName()).thenReturn("one");
        when(two.getName()).thenReturn("two");
        when(three.getName()).thenReturn("three");
        when(group1.getName()).thenReturn("group1");
        when(group2.getName()).thenReturn("group2");
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
        assertEquals(emptyList(), env.findGroupsWithIp("badIp"));
    }

    @Test
    void findGroupByName() {
        assertEquals(group2, env.getGroupWithName("group2"));
        assertThrows(IllegalArgumentException.class, () -> env.getGroupWithName("group3"));
    }

    @Test
    void findGroupWithComponentName() {
        when(group2.findComponentWithName("three")).thenReturn(of(three));

        assertEquals(of(group2), env.findGroupWithComponent("three"));
        assertEquals(empty(), env.findGroupWithComponent("not found"));
    }

    @Test
    void getAllComponents() {
        assertEquals(new ComponentsImpl(asList(one, two, three), factory2), env.getAllComponents());
    }

    @Test
    void getComponentWithName() {
        when(group1.findComponentWithName("two")).thenReturn(of(two));
        assertEquals(two, env.getComponentWithName("two"));

        assertThrows(IllegalArgumentException.class, () -> env.getComponentWithName("four"));
    }

    @Test
    void getOrCreateComponentWithName() {
        when(group1.findComponentWithName("two")).thenReturn(of(two));
        assertEquals(two, env.findComponentWithName("two"));

        Component four = mock(Component.class);
        when(factory.createComponent("four", "four", env.getName())).thenReturn(four);
        assertEquals(four, env.findComponentWithName("four"));
    }

    @Test
    void findComponentsFrom() {
        assertEquals(components(one, two, three), env.findComponentsFrom(emptyList(), emptyList()));
        assertEquals(components(one, two, three), env.findComponentsFrom(asList("group1", "group2"), emptyList()));
        assertEquals(components(one, two), env.findComponentsFrom(asList("group1", "group2"), asList("one", "two")));
        assertEquals(components(one, two), env.findComponentsFrom(singletonList("group1"), asList("one", "two")));
        assertEquals(components(one, three), env.findComponentsFrom(emptyList(), asList("one", "three")));

        assertThrows(IllegalArgumentException.class, () -> env.findComponentsFrom(singletonList("bad"), singletonList("bad")));
        assertThrows(IllegalArgumentException.class, () -> env.findComponentsFrom(singletonList("bad"), emptyList()));
        assertThrows(NullPointerException.class, () -> env.findComponentsFrom(emptyList(), singletonList("bad")));
    }

    @Test
    void testToString() {
        when(group1.toString()).thenReturn("g1");
        when(group2.toString()).thenReturn("g2");
        assertEquals("dev: [g1, g2]", env.toString());
    }

    private Components components(Component... components) {
        return new ComponentsImpl(asList(components), factory2);
    }
}