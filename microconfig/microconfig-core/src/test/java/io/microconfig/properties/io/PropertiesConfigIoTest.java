package io.microconfig.properties.io;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.microconfig.utils.ClasspathUtils.getClasspathFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropertiesConfigIoTest {
    private final PropertiesConfigIo ioService = new PropertiesConfigIo();

    @Test
    @Disabled
    void test() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("p", "p_v");
        expected.put("p2", "p2_v");
        expected.put("p3", "=p3_v");
        expected.put("p4", ":p4_v");
//        expected.put("jwt.publicKey", "-----BEGIN PUBLIC KEY-----\\" + LINE_SEPARATOR +
//                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgfjVb7pJlEdu9lDPOxmi\\" + LINE_SEPARATOR +
//                "LwIDAQAB\\" + LINE_SEPARATOR +
//                "-----END PUBLIC KEY-----");
        expected.put("empty", "");
        expected.put("empty2", "");

        Map<String, String> actual = ioService.read(new File(getClasspathFile("files"), "multiLine.properties"));
        assertEquals(expected, actual);
    }
}