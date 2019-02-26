package io.microconfig.configs.files.io.yaml;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlWriterTest {
    @Test
    void testWrite() {
        Map<String, String> initial = singletonMap("metrics.distribution.percentiles-histogram[http.server.requests]", "true");
        Map<String, Object> actual = new YamlWriter(null).toTree(initial);

        Map<String, Object> expected = singletonMap("metrics", singletonMap("distribution", singletonMap("percentiles-histogram[http.server.requests]", "true")));
        assertEquals(expected, actual);
    }

    @Test
    void testWriteInner() {
        Map<String, String> initial = new HashMap<>();
        initial.put("tfs.out.archiveDir", "dirV");
        initial.put("tfs.out", "outV");
        initial.put("tfs.out.shouldArchive", "true");


        Map<String, Object> expected = new TreeMap<>();
        Map<String, Object> level2 = new TreeMap<>();
        expected.put("tfs", level2);
        level2.put("out.archiveDir", "dirV");
        level2.put("out", "outV");
        level2.put("out.shouldArchive", "true");

        Map<String, Object> actual = new YamlWriter(null).toTree(initial);
        assertEquals(expected, actual);
    }
}