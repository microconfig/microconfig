package io.microconfig.utils;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.IoUtils.readLines;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IoUtilsTest {
    @Test
    void testReadFully() throws FileNotFoundException {
        String expected = "k1:" + LINES_SEPARATOR +
                "  k2: v2";
        assertEquals(expected, readFully(resourceFile()));
        assertEquals(expected, readFully(new FileInputStream(resourceFile())));
    }

    @Test
    void testReadLines() {
        assertEquals(asList("k1:", "  k2: v2"), readLines(resourceFile()));
    }

    public static File resourceFile() {
        return new File(IoUtils.class.getClassLoader().getResource("file.yaml").getFile());
    }
}