package io.microconfig.configs.files.io.properties;

import io.microconfig.utils.reader.FsFileReader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static io.microconfig.utils.ClasspathUtils.classpathFile;
import static io.microconfig.utils.FileUtils.LINES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesConfigIoServiceTest {
    private final PropertiesConfigIoService ioService = new PropertiesConfigIoService(new FsFileReader());

    @Test
    void test() {
        File file = classpathFile("files/propLine.properties");
        assertEquals(expectedMap(false), ioService.read(file).propertiesAsMap());
        assertEquals(expectedMap(true), ioService.read(file).escapeResolvedPropertiesAsMap());
    }

    private Map<String, String> expectedMap(boolean resolveEscape) {
        String multilineEscape = resolveEscape ? "": "\\" + LINES_SEPARATOR;
        Map<String, String> expected = new TreeMap<>();
        expected.put("p", "p_v");
        expected.put("p2", "p2_v");
        expected.put("p3", "=p3_v");
        expected.put("p4", ":p4_v");
        expected.put("jwt.publicKey", "-----BEGIN PUBLIC KEY-----" + multilineEscape +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgfjVb7pJlEdu9lDPOxmi" + multilineEscape +
                "LwIDAQAB" + multilineEscape +
                "-----END PUBLIC KEY-----");
        expected.put("empty", "");
        expected.put("empty2", "");
        return expected;
    }
}