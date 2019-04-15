package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.environments.Component.byType;
import static org.junit.jupiter.api.Assertions.*;

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