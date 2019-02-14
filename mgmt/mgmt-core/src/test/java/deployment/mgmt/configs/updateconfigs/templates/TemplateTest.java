package deployment.mgmt.configs.updateconfigs.templates;

import io.microconfig.templates.Template;
import io.microconfig.templates.TemplatePattern;
import org.junit.Test;

import static java.util.Map.of;
import static org.junit.Assert.assertEquals;

public class TemplateTest {
    private final TemplatePattern templatePattern = TemplatePattern.defaultPattern();

    @Test
    public void testSubstitution() {
        Template template = new Template("${param:default}");
        String result = template.resolvePlaceholders(of("param", "value"), templatePattern);
        assertEquals("value", result);
    }

    @Test
    public void testSubstitutionWithMissedParameter() {
        Template template = new Template("${param:default}");
        String result = template.resolvePlaceholders(of(), templatePattern);
        assertEquals("default", result);
    }

    @Test
    public void testEscapedSubstitution() {
        Template template = new Template("\\${param:default}");
        String result = template.resolvePlaceholders(of("param", "failure"), templatePattern);
        assertEquals("${param:default}", result);
    }

    @Test
    public void testExtendedPattern() {
        Template template = new Template("aaa${param}bbb\\${xxx}ccc${missing_param:---}ddd");
        String result = template.resolvePlaceholders(of("param", "***"), templatePattern);
        assertEquals("aaa***bbb${xxx}ccc---ddd", result);
    }
}