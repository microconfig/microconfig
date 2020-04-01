package io.microconfig.core.properties.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentNotFoundExceptionTest {
    @Test
    void test() {
        ComponentNotFoundException exception = new ComponentNotFoundException("p4");
        exception.withParentComponent("p3");
        exception.withParentComponent("p2");
        exception.withParentComponent("p1");

        try {
            throw exception;
        } catch (ComponentNotFoundException e) {
            assertEquals("Component 'p4' doesn't exist. Dependency chain: p1 -> p2 -> p3 -> p4", e.getMessage());
        }
    }
}