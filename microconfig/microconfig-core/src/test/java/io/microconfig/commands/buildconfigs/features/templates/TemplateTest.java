package io.microconfig.commands.buildconfigs.features.templates;

import io.microconfig.core.properties.resolver.EnvComponent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static io.microconfig.core.environments.Component.byType;
import static io.microconfig.testutils.ClasspathUtils.classpathFile;
import static io.microconfig.testutils.MicronconfigTestFactory.getPropertyResolver;
import static io.microconfig.utils.OsUtil.currentUser;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {
    private final EnvComponent envComponent = new EnvComponent(byType("th-server"), "uat");
    private final Pattern templatePattern = TemplatePattern.defaultPattern().getPattern();
    private final File source = new File("source");

    @Test
    void testResolve() {
        Consumer<String> testIp = prop -> {
            String result = new Template(source, prop).resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
            assertEquals("172.30.162.3", result);
        };

        testIp.accept("${ip:default}");
        testIp.accept("${this@ip}");
    }

    @Test
    void testMissedParameter() {
        Template template = new Template(source, "${param:default}");
        String result = template.resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
        assertEquals("default", result);
    }

    @Test
    void testEscaped() {
        Template template = new Template(source, "\\${param:default}");
        String result = template.resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
        assertEquals("${param:default}", result);
    }

    @Test
    void testExtendedPattern() {
        Template template = new Template(source, "aaa${env}bbb\\${xxx}ccc${missing_param:---}ddd");
        String result = template.resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
        assertEquals("aaauatbbb${xxx}ccc---ddd", result);
    }

    @Test
    void testEnvProperties() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        Template template = new Template(source, "${env@" + entry.getKey() + "}");
        String result = template.resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
        UnaryOperator<String> escape = v -> v.replaceAll("\\\\+", "/");
        assertEquals(escape.apply(entry.getValue()), escape.apply(result));
    }

    @Test
    void testSystemProperties() {
        Template template = new Template(source, "${system@user.name}");
        String result = template.resolvePlaceholders(envComponent, getPropertyResolver(), templatePattern);
        assertEquals(currentUser(), result);
    }

    @Test
    void testMultiLinePlaceholderInTemplate() {
        Template template = new Template(classpathFile("templates/templateWithMultiLines.yaml"));
        String result = template.resolvePlaceholders(new EnvComponent(byType("mergeLists"), "some"), getPropertyResolver(), templatePattern);
        System.out.println(result);
    }
}
