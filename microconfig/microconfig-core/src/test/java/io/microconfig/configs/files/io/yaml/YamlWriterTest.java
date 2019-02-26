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
        initial.put("cr.cf.tfs.out.archiveDir", "dirV");
        initial.put("cr.cf.tfs.out", "outV");
        initial.put("cr.cf.tfs.out.shouldArchive", "true");

        Map<String, Object> expected = new TreeMap<>();
        Map<String, Object> level2 = new TreeMap<>();
        expected.put("cr", level2);
        Map<String, Object> level3 = new TreeMap<>();
        level2.put("cf", level3);
        Map<String, Object> level4 = new TreeMap<>();
        level3.put("tfs", level4);
        level4.put("out.archiveDir", "dirV");
        level4.put("out", "outV");
        level4.put("out.shouldArchive", "true");

        Map<String, Object> actual = new YamlWriter(null).toTree(initial);
        assertEquals(expected, actual);
    }
}