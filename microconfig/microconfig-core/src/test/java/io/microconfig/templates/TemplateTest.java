package io.microconfig.templates;

import io.microconfig.properties.resolver.RootComponent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static io.microconfig.environments.Component.byType;
import static io.microconfig.utils.MicronconfigTestFactory.getPropertyResolver;
import static io.microconfig.utils.OsUtil.currentUser;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateTest {
    private final RootComponent rootComponent = new RootComponent(byType("th-server"), "uat");
    private final Pattern templatePattern = TemplatePattern.defaultPattern().getPattern();
    private final File source = new File("source");

    @Test
    void testResolve() {
        Consumer<String> testIp = prop -> {
            String result = new Template(source, prop).resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
            assertEquals("172.30.162.3", result);
        };

        testIp.accept("${ip:default}");
        testIp.accept("${this@ip}");
    }

    @Test
    void testMissedParameter() {
        Template template = new Template(source, "${param:default}");
        String result = template.resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
        assertEquals("default", result);
    }

    @Test
    void testEscaped() {
        Template template = new Template(source, "\\${param:default}");
        String result = template.resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
        assertEquals("${param:default}", result);
    }

    @Test
    void testExtendedPattern() {
        Template template = new Template(source, "aaa${env}bbb\\${xxx}ccc${missing_param:---}ddd");
        String result = template.resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
        assertEquals("aaauatbbb${xxx}ccc---ddd", result);
    }

    @Test
    void testEnvProperties() {
        Map.Entry<String, String> entry = System.getenv().entrySet().iterator().next();
        Template template = new Template(source, "${env@" + entry.getKey() + "}");
        String result = template.resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
        assertEquals(entry.getValue(), result);
    }

    @Test
    void testSystemProperties() {
        Template template = new Template(source, "${system@user.name}");
        String result = template.resolvePlaceholders(rootComponent, getPropertyResolver(), templatePattern);
        assertEquals(currentUser(), result);
    }
}
