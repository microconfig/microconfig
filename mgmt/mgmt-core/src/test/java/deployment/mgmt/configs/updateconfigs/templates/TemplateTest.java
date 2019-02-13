package deployment.mgmt.configs.updateconfigs.templates;

import io.microconfig.templates.Template;
import org.junit.Test;

import static java.util.Map.of;
import static org.junit.Assert.assertEquals;

public class TemplateTest {
    @Test
    public void testSubstitution() {
        Template template = new Template("${param:default}");
        String result = template.resolvePlaceholders(of("param", "value"));
        assertEquals("value", result);
    }

    @Test
    public void testSubstitutionWithMissedParameter() {
        Template template = new Template("${param:default}");
        String result = template.resolvePlaceholders(of());
        assertEquals("default", result);
    }

    @Test
    public void testEscapedSubstitution() {
        Template template = new Template("\\${param:default}");
        String result = template.resolvePlaceholders(of("param", "failure"));
        assertEquals("${param:default}", result);
    }

    @Test
    public void testExtendedPattern() {
        Template template = new Template("aaa${param}bbb\\${xxx}ccc${missing_param:---}ddd");
        String result = template.resolvePlaceholders(of("param", "***"));
        assertEquals("aaa***bbb${xxx}ccc---ddd", result);
    }
}