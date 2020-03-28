package io.microconfig.core.properties.templates;

import io.microconfig.core.Microconfig;
import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {
    DeclaringComponent root = new DeclaringComponentImpl("app", "th-server", "uat");
    Pattern templatePattern = defaultPattern().getPattern();
    File source = new File("source");

    @ParameterizedTest
    @ValueSource(strings = {"${ip:default}", "${this@ip}"})
    void resolveIp(String placeholder) {
        String result = new Template(source, templatePattern, placeholder).resolveBy(resolver(), root).getContent();
        assertEquals("172.30.162.3", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", ""})
    void resolveDefaultValue(String defaultValue) {
        Template template = new Template(source, templatePattern, "${param:" + defaultValue + "}");
        String result = template.resolveBy(resolver(), root).getContent();
        assertEquals(defaultValue, result);
    }

    @Test
    void testEscaped() {
        Template template = new Template(source, templatePattern, "\\${param:default}");
        String result = template.resolveBy(resolver(), root).getContent();
        assertEquals("${param:default}", result);
    }

    @Test
    void testExtendedPattern() {
        Template template = new Template(source, templatePattern, "aaa${env}bbb\\${xxx}ccc${missing_param:---}ddd");
        String result = template.resolveBy(resolver(), root).getContent();
        assertEquals("aaauatbbb${xxx}ccc---ddd", result);
    }

    @Test
    void testEnvProperties() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        Template template = new Template(source, templatePattern, "${env@" + entry.getKey() + "}");
        String result = template.resolveBy(resolver(), root).getContent();
        UnaryOperator<String> escape = v -> v.replaceAll("\\\\+", "/");
        assertEquals(escape.apply(entry.getValue()), escape.apply(result));
    }

    @Test
    void testSystemProperties() {
        Template template = new Template(source, templatePattern, "${system@user.name}");
        String result = template.resolveBy(resolver(), root).getContent();
        assertEquals(System.getProperty("user.name"), result);
    }

    @Test
    void testMultiLinePlaceholderInTemplate() {
        Template template = new Template(classpathFile("templates/templateWithMultiLines.yaml"), templatePattern);
        String result = template.resolveBy(resolver(), new DeclaringComponentImpl("app", "mergeLists", "some")).getContent();
        assertEquals("key1:\n" +
                "  key2:\n" +
                "    key3:\n" +
                "      - name: n1\n" +
                "        v: v2\n" +
                "      - v1\n" +
                "      - v2\n" +
                "      - v3\n" +
                "      - v4\n" +
                "      - v5\n" +
                "      \n" +
                "\n" +
                "\n" +
                "key10: mergeLists", result);
    }

    private Resolver resolver() {
        return Microconfig.searchConfigsIn(classpathFile("repo")).resolver();
    }
}
