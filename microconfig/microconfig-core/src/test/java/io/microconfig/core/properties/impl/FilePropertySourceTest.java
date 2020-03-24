package io.microconfig.core.properties.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.core.properties.impl.FilePropertySource.fileSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePropertySourceTest {
    File file = new File("component/config.yaml");
    FilePropertySource source = fileSource(file, 0, true);

    @Test
    void getters() {
        assertEquals(file, source.getSource());
        assertEquals(0, source.getLineNumber());
        assertTrue(source.isYaml());
    }

    @Test
    void constructor() {
        assertEquals(source, new FilePropertySource(file, 0, true));
    }

    @Test
    void parentComponent() {
        assertEquals("component", source.getComponent());
    }

    @Test
    void string() {
        assertEquals(file.getAbsolutePath() + ":1", source.toString());
    }
}