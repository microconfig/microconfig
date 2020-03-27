package io.microconfig.core.environments;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
        assertEquals(empty(), nullableIp.getIp());

        String ip = "127.0.0.1";
        ComponentGroup ipGroup = new ComponentGroupImpl("group", ip, components);
        assertEquals(of(ip), ipGroup.getIp());
    }

    @Test
    void findComponentByName() {
        when(one.getName()).thenReturn("one");
        when(two.getName()).thenReturn("two");
        when(components.asList()).thenReturn(asList(one, two));

        ComponentGroup group = new ComponentGroupImpl("group", null, components);
        assertEquals(of(two), group.findComponentWithName("two"));
        assertEquals(empty(), group.findComponentWithName("missing"));
    }

    @Test
    void string() {
        when(components.toString()).thenReturn("[]");
        ComponentGroup group = new ComponentGroupImpl("groupName", null, components);

        assertEquals("groupName: []", group.toString());
    }
}