package io.microconfig;

import org.junit.jupiter.api.Test;

import java.io.File;

import static io.microconfig.MicroconfigParams.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicroconfigParamsTest {
    MicroconfigParams params = parse(
            "-r", "configs/repo",
            "-d", "destination",
            "-e", "dev",
            "-envs", "dev, test",
            "-g", "g1,g2",
            "-s", "s1, s2",
            "-output", "json"
    );
    MicroconfigParams empty = MicroconfigParams.parse();

    @Test
    void rootDir() {
        assertEquals(new File("configs/repo"), params.rootDir());
    }

    @Test
    void destinationDir() {
        assertEquals("destination", params.destinationDir());
        assertEquals("build", empty.destinationDir());
    }

    @Test
    void env() {
        assertEquals("dev", params.env());
    }

    @Test
    void envs() {
        assertEquals(2,params.envs().size());
        assertEquals("dev", params.envs().get(0));
        assertEquals("test", params.envs().get(1));
    }

    @Test
    void groups() {
        assertEquals(asList("g1", "g2"), params.groups());
        assertEquals(emptyList(), empty.groups());
    }

    @Test
    void services() {
        assertEquals(asList("s1", "s2"), params.services());
        assertEquals(emptyList(), empty.services());
    }

    @Test
    void jsonOutput() {
        assertFalse(empty.jsonOutput());
        assertTrue(params.jsonOutput());
    }

    @Test
    void testVersion() {
        assertTrue(parse("-v").version());
    }
}