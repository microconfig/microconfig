package io.microconfig.core.properties.io.ioservice.yaml;

import io.microconfig.utils.reader.FsFilesReader;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.testutils.ClasspathUtils.read;
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

        doCompare("files/yaml/tree/escapedResult.yaml", initial);
    }

    @Test
    void testDoubleEscaped() {
        Map<String, String> initial = new HashMap<String, String>() {
            {
                put("a.b[c[d.v].1.2]", "v1");
                put("b.\"b.\"c.c2\".d\"", "v2");
            }
        };

        doCompare("files/yaml/tree/doubleEscapedResult.yaml", initial);
    }

    @Test
    void testSortOrder() {
        assertEquals(read("files/yaml/sortOrder/result.yaml"), toYaml("files/yaml/sortOrder/initial.yaml"));
    }

    @Test
    void testList() {
        assertEquals(read("files/yaml/list/resultList.yaml"), toYaml("files/yaml/list/list.yaml").replace("services: ", "services:"));
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

        Map<String, Object> actual = new YamlTreeImpl.TreeCreator().toTree(initial);
        assertEquals(expected, actual);
    }

    private void doCompare(String expected, Map<String, String> initial) {
        assertEquals(read(expected), new YamlTreeImpl().toYaml(initial));
    }

    private String toYaml(String file) {
        return new YamlTreeImpl().toYaml(
                new YamlReader(classpathFile(file), new FsFilesReader()).propertiesAsMap()
        );
    }
}