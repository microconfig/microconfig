package io.microconfig.commands.buildconfig.features.templates;

import org.junit.jupiter.api.Test;

import static io.microconfig.commands.buildconfig.features.templates.TemplatePattern.DEFAULT_TEMPLATE_PREFIX;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

class TemplatePatternTest {
    private TemplatePattern templatePattern = TemplatePattern.defaultPattern().withTemplatePrefixes(asList(DEFAULT_TEMPLATE_PREFIX, "mgmt.template."));
    private String microTemplate = "microconfig.template.logback.fromFile";
    private String mgmtTemplate = "mgmt.template.t2.toFile";

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
    }
}