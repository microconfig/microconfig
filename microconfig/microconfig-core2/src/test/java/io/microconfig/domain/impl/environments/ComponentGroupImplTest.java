package io.microconfig.domain.impl.environments;

import io.microconfig.domain.Component;
import io.microconfig.domain.ComponentGroup;
import io.microconfig.domain.Components;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentGroupImplTest {

    Component one = mock(Component.class);
    Component two = mock(Component.class);
    Components components = mock(Components.class);

    @Test
    void ipOptionalWrapping() {
        ComponentGroup nullableIp = new ComponentGroupImpl("null", null, components);
        assertEquals(Optional.empty(), nullableIp.getIp());

        String ip = "127.0.0.1";
        ComponentGroup ipGroup = new ComponentGroupImpl("group", ip, components);
        assertEquals(Optional.of(ip), ipGroup.getIp());
    }

    @Test
    void findComponentByName() {
        when(one.getName()).thenReturn("one");
        when(two.getName()).thenReturn("two");
        when(components.asList()).thenReturn(asList(one, two));

        ComponentGroupImpl group = new ComponentGroupImpl("group", null, components);

        Optional<Component> result = group.findComponentWithName("two");
        assertEquals(Optional.of(two), result);
    }

    @Test
    void string() {
        when(components.toString()).thenReturn("[]");
        ComponentGroupImpl group = new ComponentGroupImpl("groupName", null, components);

        String expected = "groupName: []";
        assertEquals(expected, group.toString());
    }

}