package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.environments.Component.byType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultServiceDirPropertyTest {
    private String path = "c:\\components";
    private ResultServiceDirProperty resultServiceDirProperty;

    @BeforeEach
    void setUp() {
        resultServiceDirProperty = new ResultServiceDirProperty(new File(path));
    }

    @Test
    void value() {
        assertEquals(new File(path).getAbsolutePath().replace("\\", "/") + "/c2", resultServiceDirProperty.value(byType("c2")).get());
    }
}