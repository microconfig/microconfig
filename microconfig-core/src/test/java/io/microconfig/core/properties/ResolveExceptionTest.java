package io.microconfig.core.properties;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static io.microconfig.core.properties.PropertyImpl.property;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResolveExceptionTest {
    DeclaringComponent current = new DeclaringComponentImpl("ct2", "current", "e1");
    DeclaringComponent root = new DeclaringComponentImpl("ct", "root", "e");
    Property p = property("key", "value", PROPERTIES, current);

    @Test
    void withoutCause() {
        ResolveException exception = new ResolveException(current, root, "Can't resolve placeholder");
        assertEquals("Can't build configs for root component 'root[e]'.\n" +
                "Exception in\n" +
                "\tcurrent[e1]'\n" +
                "Can't resolve placeholder", exception.getMessage().trim());

        exception.setProperty(p);
        assertEquals("Can't build configs for root component 'root[e]'.\n" +
                "Exception in\n" +
                "\tcurrent[e1]'\n" +
                "\tkey=value\n" +
                "Can't resolve placeholder", exception.getMessage().trim());
    }

    @Test
    void withCauseMessage() {
        ResolveException exception = new ResolveException(current, root, "Can't resolve placeholder", new NullPointerException("NPE"));
        assertEquals("Can't build configs for root component 'root[e]'.\n" +
                "Exception in\n" +
                "\tcurrent[e1]'\n" +
                "Can't resolve placeholder\n" +
                "Cause: NPE", exception.getMessage());

        exception.setProperty(p);
        assertEquals("Can't build configs for root component 'root[e]'.\n" +
                "Exception in\n" +
                "\tcurrent[e1]'\n" +
                "\tkey=value\n" +
                "Can't resolve placeholder\n" +
                "Cause: NPE", exception.getMessage());
    }
}