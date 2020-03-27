package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.utils.FileUtils.getExtension;
import static io.microconfig.utils.FileUtils.getName;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FileUtilsTest {
    @Test
    void nameAndExtension() {
        File f1 = new File("file.yaml");
        File f2 = new File("file.file2.yaml");
        File f3 = new File("file");

        assertEquals("file", getName(f1));
        assertEquals("file.file2", getName(f2));
        assertEquals("file", getName(f3));

        assertEquals(".yaml", getExtension(f1));
        assertEquals(".yaml", getExtension(f2));
        assertEquals("", getExtension(f3));
    }
}