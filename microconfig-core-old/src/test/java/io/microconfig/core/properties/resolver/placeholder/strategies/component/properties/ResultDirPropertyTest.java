package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.environments.Component.byType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultDirPropertyTest {
    private String path = "c:\\components";
    private ResultDirProperty resultDirProperty;

    @BeforeEach
    void setUp() {
        resultDirProperty = new ResultDirProperty(new File(path));
    }

    @Test
    void value() {
        assertEquals(new File(path).getAbsolutePath().replace("\\", "/") + "/c2", resultDirProperty.value(byType("c2")).get());
    }
}