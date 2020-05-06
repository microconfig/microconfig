package io.microconfig.io;

import org.junit.jupiter.api.Test;

import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.IoUtilsTest.resourceFile;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertEquals;


class DumpedFsReaderTest {
    FsReader reader = new DumpedFsReader();

    @Test
    void readFully() {
        String expected = "k1:" + LINES_SEPARATOR +
                "  k2: v2";
        assertEquals(expected, reader.readFully(resourceFile()));
    }

    @Test
    void readLines() {
        assertEquals(asList("k1:", "  k2: v2"), reader.readLines(resourceFile()));
    }

    @Test
    void firstLineOf() {
        assertEquals(of("  k2: v2"), reader.firstLineOf(resourceFile(), s-> s.contains("k2")));
        assertEquals(empty(), reader.firstLineOf(resourceFile(), s-> s.contains("eeee")));
    }
}