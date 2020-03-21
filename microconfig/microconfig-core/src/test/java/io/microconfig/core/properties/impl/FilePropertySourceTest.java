package io.microconfig.core.properties.impl;

import io.microconfig.core.properties.PropertySource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static io.microconfig.core.properties.impl.FilePropertySource.fileSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class FilePropertySourceTest {
    File file = spy(new File("component/config.yaml"));
    FilePropertySource source = fileSource(file, 0, true);

    @Test
    void getters() {
        assertEquals(file, source.getSource());
        assertEquals(0, source.getLineNumber());
        assertTrue( source.isYaml());
    }

    @Test
    void constructor() {
        assertEquals(source, new FilePropertySource(file, 0, true));
    }

    @Test
    void parentComponent() {
        assertEquals("component", source.getDeclaringComponent());
    }

    @Test
    void string() {
        when(file.getAbsolutePath()).thenReturn("path/component/config.yaml");
        assertEquals("path/component/config.yaml:1", source.toString());
    }
}