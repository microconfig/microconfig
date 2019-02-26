package io.microconfig.configs.files.io.yaml;

import org.junit.jupiter.api.Test;

import java.util.Map;

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
}