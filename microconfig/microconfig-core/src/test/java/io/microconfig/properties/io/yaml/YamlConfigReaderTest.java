package io.microconfig.properties.io.yaml;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.utils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlConfigReaderTest {
    private final YamlConfigIoService yaml = new YamlConfigIoService();

    @Test
    void testSimpleYaml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("p0", "p0v");
        map.put("p1.p2.p3.p4.p5", "p5v");
        map.put("server.port", "8080");
        map.put("name", "");
        map.put("name.name2", "");
        map.put("displayName", "dv");

        assertEquals(map, yaml.read(classpathFile("files/yaml/simple.yaml")).asMap());
    }

    @Test
    void testInnerYaml() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("p1.p2.p3.p4.p5", "p5v");
        map.put("p1.p2.p3.p4.p99", "");
        map.put("p1.p2.p3.p4.p100", "p100v");
        map.put("p1.p2.p3_2", "p3_2v");
        map.put("p1.p2.p3_2.p4_2.p5", "p5_v");
        map.put("p1.p2.p3_2.p4_2.p6", "p6_v");
        map.put("p1.p2.p3_2.p35_2", "p3_2.p35_2_v");
        map.put("p1.p2_2", "p2_2");
        map.put("p1.p6", "p6v");
        map.put("p1.p7", "p7v");
        map.put("p1.p2_3", "p2_3v");
        map.put("p9", "p9v");

        assertEquals(map, yaml.read(classpathFile("files/yaml/inner.yaml")).asMap());
    }

    @Test
    void testMultiline() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("psp.adyen.payment-method-list",
                LINES_SEPARATOR +
                "- name: bancontact-card" + LINES_SEPARATOR +
                "  displayName: Bancontact (card)" + LINES_SEPARATOR +
                "  pspName: bcmc" + LINES_SEPARATOR +
                "  fee: 0" + LINES_SEPARATOR +
                "  countryCodes: BE" + LINES_SEPARATOR +
                "  enabled: true" + LINES_SEPARATOR +
                LINES_SEPARATOR +
                "- name: bancontact-mobile" + LINES_SEPARATOR +
                "  displayName: Bancontact (mobile)" + LINES_SEPARATOR +
                "  pspName: bcmc_mobile" + LINES_SEPARATOR +
                "  fee: 0" + LINES_SEPARATOR +
                "  countryCodes: BE" + LINES_SEPARATOR +
                "  enabled: true"
        );

        assertEquals(map, yaml.read(classpathFile("files/yaml/multilines.yaml")).asMap());
    }
}