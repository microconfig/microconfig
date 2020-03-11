package io.microconfig.core.properties.io.ioservice.yaml;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.io.ioservice.ConfigReader;
import io.microconfig.core.properties.sources.FileSource;
import io.microconfig.service.io.FsIo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlReaderTest {
    private final YamlConfigIoService yaml = new YamlConfigIoService(new FsIo());

    @Test
    void testSimpleYaml() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("p0", "p0v");
        expected.put("p1.p2.p3.p4.p5", "p5v");
        expected.put("server.port", "8080");
        expected.put("name", "");
        expected.put("name.name2", "");
        expected.put("displayName", "dv");

        ConfigReader read = yaml.read(classpathFile("files/yaml/parse/simple.yaml"));
        assertEquals(expected, read.propertiesAsMap());

        List<Property> properties = read.properties("");
        assertEquals(0, ((FileSource) properties.get(0).getSource()).getLineNumber());
        assertEquals(6, ((FileSource) properties.get(1).getSource()).getLineNumber());
        assertEquals(8, ((FileSource) properties.get(2).getSource()).getLineNumber());
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

        assertEquals(map, yaml.read(classpathFile("files/yaml/parse/inner.yaml")).propertiesAsMap());
    }

    @Test
    void testInnerYaml2() {
        Map<String, String> map = new TreeMap<>();
        map.put("cr.cf.tfs.out", "outV");
        map.put("cr.cf.tfs.out.shouldArchive", "true");
        map.put("cr.cf.tfs.out.archiveDir", "dirV");

        map.put("cr2.cf.tfs.out", "outV");
        map.put("cr2.cf.tfs.out.shouldArchive", "true");
        map.put("cr2.cf.tfs.out.archiveDir", "dirV");

        assertEquals(map, yaml.read(classpathFile("files/yaml/parse/inner2.yaml")).propertiesAsMap());
    }

    @Test
    void testMultiline() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("management.endpoints", "");
        map.put("psp.adyen.dodo", "haha");
        map.put("psp.adyen.payment-method-list",
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
        map.put("psp.adyen.value", "v2");
        map.put("server.port", "8080");

        Map<String, String> expected = yaml.read(classpathFile("files/yaml/parse/multilines.yaml")).propertiesAsMap();
        assertEquals(map, expected);
    }
}