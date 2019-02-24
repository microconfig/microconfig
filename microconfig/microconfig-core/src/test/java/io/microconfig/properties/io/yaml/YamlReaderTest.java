package io.microconfig.properties.io.yaml;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.ClasspathUtils.getClasspathFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlReaderTest {
    private final YamlReader reader = new YamlReader();

    @Test
    void parseSimple() {
        File file = getClasspathFile("test-props/components/yaml/simple.yaml");
        assertEquals(simpleMap(), reader.readAsFlatMap(file));
    }

    private Map<String, String> simpleMap() {
        Map<String, String> map = new TreeMap<>();
        map.put("server.port", "8080");
        map.put("name", "");
        map.put("name.name2", "");
        map.put("displayName", "dv");
        map.put("p1.p2.p3.p4.p5", "p5v");
        map.put("p9", "p9v");
        return map;
    }
}