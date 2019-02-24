package io.microconfig.properties.io.yaml;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.utils.ClasspathUtils.classpathFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlReaderTest {
    private final YamlReader reader = new YamlReader();

    @Test
    void testSimpleYaml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("p0", "p0v");
        map.put("p1.p2.p3.p4.p5", "p5v");
        map.put("server.port", "8080");
        map.put("name", "");
        map.put("name.name2", "");
        map.put("displayName", "dv");

        assertEquals(map, reader.readAsFlatMap(classpathFile("test-props/components/yaml/simple.yaml")));
    }

    @Test
    void testInnerYaml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("p1.p2.p3.p4.p5", "p5v");
        map.put("p1.p2.p3.p4.p99", "");
        map.put("p1.p2.p3.p4.p100", "p100v");
        map.put("p1.p2.p3_2", "p3_2v");
        map.put("p1.p2.p3_2.p4_2", "");
        map.put("p1.p2.p3_2.p4_2.p5", "p5_v");
        map.put("p1.p2.p3_2.p4_2.p6", "p6_v");
        map.put("p1.p2.p3_2.p35_2", "p3_2.p35_2_v");
        map.put("p1.p2_2", "p2_2");
        map.put("p1.p2_2.p6", "p6v");
        map.put("p1.p2_2.p7", "p7v");
        map.put("p1.p2_3", "p2_3v");
        map.put("p9", "p9v");

        assertEquals(map, reader.readAsFlatMap(classpathFile("test-props/components/yaml/inner.yaml")));
    }
}