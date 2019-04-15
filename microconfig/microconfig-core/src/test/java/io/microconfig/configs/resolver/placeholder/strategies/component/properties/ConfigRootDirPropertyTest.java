package io.microconfig.configs.resolver.placeholder.strategies.component.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static io.microconfig.environments.Component.byType;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ConfigRootDirPropertyTest {
    private String path = "c:\\configs\\root";
    private ConfigRootDirProperty configRootDirProperty;

    @BeforeEach
    void setUp() {
        configRootDirProperty = new ConfigRootDirProperty(new File(path));
    }

    @Test
    void value() {
        assertEquals(new File(path).getAbsolutePath().replace("\\", "/"), configRootDirProperty.value(byType("c2")).get());
    }
}