package io.microconfig.templates;

import io.microconfig.properties.Property;
import io.microconfig.properties.Property.Source;
import io.microconfig.properties.resolver.PropertyResolver;
import io.microconfig.properties.resolver.RootComponent;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.properties.resolver.placeholder.Placeholder.isPlaceholder;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.warn;
import static java.util.regex.Matcher.quoteReplacement;

public class Template {
    private final String text;
    private final File source;

    public Template(File source) {
        this.text = readFully(source);
        this.source = source;
    }

    public String resolvePlaceholders(RootComponent currentComponent, PropertyResolver propertyResolver, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return text;

        StringBuilder result = new StringBuilder();
        do {
            doResolve(m, result, propertyResolver, currentComponent);
        } while (m.find());
        m.appendTail(result);
        return result.toString();
    }

    private void doResolve(Matcher m, StringBuilder result, PropertyResolver propertyResolver, RootComponent currentComponent) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String value = resolveValue(currentComponent, propertyResolver, m.group("name"), m.group("defvalue"));
        m.appendReplacement(result, value == null ? "$0" : quoteReplacement(value));
    }

    private String resolveValue(RootComponent currentComponent, PropertyResolver propertyResolver, String value, String defvalue) {
        String placeholder = toPlaceholder(value, defvalue);
        Property property = new Property("key", placeholder, currentComponent.getRootEnv(), new Source(currentComponent.getRootComponent(), source.getAbsolutePath()));
        try {
            return propertyResolver.resolve(property, currentComponent);
        } catch (RuntimeException e) {
            warn("Template placeholder error: " + e.getMessage());
            return null;
        }
    }

    private String toPlaceholder(String value, String originalDevValue) {
        if (isPlaceholder(value)) return value;

        String defaultValue = originalDevValue == null ? "" : ":" + originalDevValue;
        return "${this@" + value + defaultValue + "}";
    }
}