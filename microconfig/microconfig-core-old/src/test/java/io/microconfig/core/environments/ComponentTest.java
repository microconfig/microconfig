package io.microconfig.core.environments;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.environments.Component.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentTest {
    private String alias = "zuul2";
    private String type = "zuul";

    @Test
    void testFactoryMethods() {
        assertEquals(new Component(alias, type), byNameAndType(alias, type));
        assertEquals(new Component(type, type), byType(type));
        assertEquals(new Component("eureka", "eureka"), bySourceFile(new File("components/eureka/application.prod.yaml")));
    }

    @Test
    void testToString() {
        assertEquals(alias + ":" + type, byNameAndType(alias, type).toString());
    }
}