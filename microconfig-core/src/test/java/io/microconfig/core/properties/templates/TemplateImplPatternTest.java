package io.microconfig.core.properties.templates;

import org.junit.jupiter.api.Test;

import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class TemplateImplPatternTest {
    TemplatePattern templatePattern = defaultPattern()
            .withTemplatePrefixes(asList("microconfig.template.", "mgmt.template."));
    String microTemplate = "microconfig.template.logback.fromFile";
    String mgmtTemplate = "mgmt.template.t2.toFile";

    @Test
    void startsWithTemplatePrefix() {
        assertFalse(templatePattern.startsWithTemplatePrefix("microconfig"));
        assertTrue(templatePattern.startsWithTemplatePrefix(microTemplate));
        assertTrue(templatePattern.startsWithTemplatePrefix(mgmtTemplate));
    }

    @Test
    void extractTemplateName() {
        assertEquals("logback", templatePattern.extractTemplateName(microTemplate));
        assertEquals("t2", templatePattern.extractTemplateName(mgmtTemplate));
        assertThrows(IllegalArgumentException.class, () -> templatePattern.extractTemplateName("bad"));
    }
}