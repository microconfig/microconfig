package deployment.mgmt.configs.updateconfigs.templates;

import io.microconfig.templates.Template;
import io.microconfig.templates.TemplatePattern;
import org.junit.jupiter.api.Test;

import static io.microconfig.utils.OsUtil.currentUser;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void testEnvProperties() {
        Template template = new Template("${env@PATH}");
        String result = template.resolvePlaceholders(of(), templatePattern);
        assertEquals(System.getenv("PATH"), result);
    }

    @Test
    public void testSystemProperties() {
        Template template = new Template("${system@user.name}");
        String result = template.resolvePlaceholders(of(), templatePattern);
        assertEquals(currentUser(), result);
    }
}