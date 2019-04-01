package io.microconfig.configs.io.ioservice.yaml;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.ClasspathUtils.read;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlTreeImplTest {
    @Test
    void testEscaped() {
        Map<String, String> initial = new HashMap<String, String>() {
            {
                put("metrics.distribution.percentiles-histogram[http.server.requests]", "true");
                put("metadata.annotations.\"kubernetes.io/ingress.class\"", "internal");
                put("a.b[c.d]", "a");
                put("b.b]c.d[", "a2");
            }
        };

        doCompare("files/tree/escapedResult.yaml", initial);
    }

    @Test
    void testDoubleEscaped() {
        Map<String, String> initial = new HashMap<String, String>() {
            {
                put("a.b[c[d.v].1.2]", "v1");
                put("b.\"b.\"c.c2\".d\"", "v2");
            }
        };

        doCompare("files/tree/doubleEscapedResult.yaml", initial);
    }

    private void doCompare(String expected, Map<String, String> initial) {
        assertEquals(read(expected).replace("\n", LINES_SEPARATOR), new YamlTreeImpl().toYaml(initial));
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

        Map<String, Object> actual = new YamlTreeImpl().toTree(initial);
        assertEquals(expected, actual);
    }
}