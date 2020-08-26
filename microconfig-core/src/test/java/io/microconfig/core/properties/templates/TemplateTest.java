package io.microconfig.core.properties.templates;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.DeclaringComponentImpl;
import io.microconfig.core.properties.Resolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static io.microconfig.core.ClasspathReader.classpathFile;
import static io.microconfig.core.Microconfig.searchConfigsIn;
import static io.microconfig.core.properties.templates.TemplatePattern.defaultPattern;
import static io.microconfig.utils.StringUtils.toUnixPathSeparator;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {
    DeclaringComponent root = new DeclaringComponentImpl("app", "th-server", "uat");
    Pattern templatePattern = defaultPattern().getPlaceholderPattern();
    File source = new File("source");

    @ParameterizedTest
    @ValueSource(strings = {"${ip:default}", "${this@ip}"})
    void resolveIp(String placeholder) {
        resolve(placeholder, "172.30.162.3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", ""})
    void resolveDefaultValue(String defaultValue) {
        resolve("${param:" + defaultValue + "}", defaultValue);
    }

    @Test
    void testEscaped() {
        resolve("\\${param:default}", "${param:default}");
    }

    @Test
    void testExtendedPattern() {
        resolve("aaa${env}bbb\\${xxx}ccc${missing_param:---}ddd", "aaauatbbb${xxx}ccc---ddd");
    }

    @Test
    void templateNameWithoutBrackets() {
        Template template = new Template("name[12]", source, TemplatePattern.DEFAULT_PATTERN, "content");
        assertEquals("name", template.templateNameWithoutBrackets());
    }

    @Test
    void testEnvProperties() {
        Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        String result = new Template("name", source, templatePattern, "${env@" + entry.getKey() + "}").
                resolveBy(resolver(), root).getContent();
        UnaryOperator<String> escape = v -> v.replaceAll("\\\\+", "/");
        assertEquals(escape.apply(entry.getValue()), escape.apply(result));
    }

    @Test
    void testSystemProperties() {
        resolve("${system@user.name}", System.getProperty("user.name"));
    }

    @Test
    void testMultiLinePlaceholderInTemplate() {
        String result = new Template("name", classpathFile("templates/templateWithMultiLines.yaml"), templatePattern)
                .resolveBy(resolver(), new DeclaringComponentImpl("app", "mergeLists", "some"))
                .getContent();
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
                "key10: mergeLists", toUnixPathSeparator(result));
    }

    private Resolver resolver() {
        return searchConfigsIn(classpathFile("repo")).resolver();
    }

    private void resolve(String placeholder, String expected) {
        Template template = new Template("name", source, templatePattern, placeholder);
        String result = template.resolveBy(resolver(), root).getContent();
        assertEquals(expected, result);
    }
}
