package io.microconfig.core.properties.resolver.placeholder.strategies.component.properties;

import io.microconfig.core.properties.io.tree.ComponentTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static io.microconfig.core.environments.Component.byType;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentConfigDirPropertyTest {
    @Mock
    private ComponentTree componentTree;
    private ComponentConfigDirProperty componentConfigDirProperty;

    @BeforeEach
    void setUp() {
        componentConfigDirProperty = new ComponentConfigDirProperty(componentTree);
    }

    @Test
    void value() {
        String c1 = "c1";
        String path = "\\configs\\root";
        when(componentTree.getFolder(c1)).thenReturn(of(new File(path)));
        assertEquals(new File(path).getAbsolutePath().replace("\\", "/"), componentConfigDirProperty.value(byType(c1)).get());
    }
}