package io.microconfig.core.properties.io.yaml;

import io.microconfig.io.DumpedFsReader;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.ClasspathReader.read;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static io.microconfig.utils.StringUtils.toUnixPathSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        doCompare("configFormats/yaml/tree/escapedResult.yaml", initial);
    }

    @Test
    void testDoubleEscaped() {
        Map<String, String> initial = new HashMap<String, String>() {
            {
                put("a.b[c[d.v].1.2]", "v1");
                put("b.\"b.\"c.c2\".d\"", "v2");
            }
        };

        doCompare("configFormats/yaml/tree/doubleEscapedResult.yaml", initial);
    }

    @Test
    void testSortOrder() {
        assertStringEquals(
                read("configFormats/yaml/sortOrder/result.yaml"),
                toYaml("configFormats/yaml/sortOrder/initial.yaml")
        );
    }

    @Test
    void testList() {
        assertStringEquals(
                read("configFormats/yaml/list/resultList.yaml"),
                toYaml("configFormats/yaml/list/list.yaml").replace("services: ", "services:")
        );
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

    @Test
    void writeIndentationIndicatorWhenMultilineValueStartsWithSpaces() {
        Map<String, String> values = Map.of(
                "indented", "  a" + LINES_SEPARATOR + "b",
                "afterEmptyLine", LINES_SEPARATOR + "  a" + LINES_SEPARATOR + "b",
                "nested.indented", "  a" + LINES_SEPARATOR + "b",
                "plain", "a" + LINES_SEPARATOR + "  b"
        );

        String yaml = new YamlTreeImpl().toYaml(values);
        Map<?, ?> actual = new Yaml().load(yaml);
        assertEquals("  a\nb\n", actual.get("indented"));
        assertEquals("\n  a\nb\n", actual.get("afterEmptyLine"));
        assertEquals("  a\nb\n", ((Map<?, ?>) actual.get("nested")).get("indented"));
        assertEquals("a\n  b\n", actual.get("plain"));
        assertTrue(yaml.contains("indented: |2"));
        assertTrue(yaml.contains("plain: |" + LINES_SEPARATOR));
    }

    @Test
    void indentMultilineValuesOfConflictingKeysByTheirDepthInTheTree() {
        Map<String, String> values = Map.of(
                "section", "value",
                "section.child", "  a" + LINES_SEPARATOR + "b",
                "section.list", "- x" + LINES_SEPARATOR + "- y",
                "outer.inner", "value",
                "outer.inner.child", "  c" + LINES_SEPARATOR + "d"
        );

        Map<?, ?> actual = new Yaml().load(new YamlTreeImpl().toYaml(values));
        assertEquals("  a\nb\n", actual.get("section.child"));
        assertEquals(java.util.List.of("x", "y"), actual.get("section.list"));
        assertEquals("  c\nd\n", ((Map<?, ?>) actual.get("outer")).get("inner.child"));
    }

    @Test
    void splitValuesByBothLineBreaksOnWindowsAndKeepUnixAsIs() {
        assertEquals(java.util.List.of("a", "b", "c"), java.util.List.of(YamlTreeImpl.lineBreakFor("\r\n").split("a\r\nb\nc")));
        assertEquals(java.util.List.of("a\r", "b"), java.util.List.of(YamlTreeImpl.lineBreakFor("\n").split("a\r\nb")));
    }

    private String toYaml(String file) {
        return new YamlTreeImpl().toYaml(
                new YamlReader(classpathFile(file), new DumpedFsReader()).propertiesAsMap()
        );
    }

    private void doCompare(String expected, Map<String, String> initial) {
        assertStringEquals(read(expected), new YamlTreeImpl().toYaml(initial));
    }

    private void assertStringEquals(String expected, String actual) {
        assertEquals(
                toUnixPathSeparator(expected),
                toUnixPathSeparator(actual)
        );
    }
}
