package io.microconfig.properties.provider;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class IncludeTest {
    @Test
    void testSingleComponentParse() {
        testSingeInclude("#include  zeus[uat]", "uat");
        testSingeInclude("#@Include   zeus[uat]", "uat");
        testSingeInclude("#@Include  zeus", "dev");
    }

    @Test
    void testMultipleComponentsParse() {
        testMultipleComponents(Include.parse("#include service_discovery,    gateway[uat]", "prod"));
        testMultipleComponents(Include.parse("#@Include       service_discovery,     gateway[uat]", "prod"));
    }

    @Test
    void testDontMatch() {
        assertFalse(Include.isInclude("include zeus[uat]"));
        assertFalse(Include.isInclude("#iclude zeus"));
    }

    private void testSingeInclude(String line, String env) {
        List<Include> include = Include.parse(line, "dev");
        assertEquals(singletonList(new Include("zeus", env)), include);
    }

    private void testMultipleComponents(List<Include> includes) {
        assertEquals(asList(new Include("service_discovery", "prod"), new Include("gateway", "uat")), includes);
    }
}
