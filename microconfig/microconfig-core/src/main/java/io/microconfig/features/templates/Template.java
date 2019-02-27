package io.microconfig.features.templates;

import io.microconfig.configs.Property;
import io.microconfig.configs.resolver.EnvComponent;
import io.microconfig.configs.resolver.PropertyResolver;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.configs.Property.tempProperty;
import static io.microconfig.configs.PropertySource.specialSource;
import static io.microconfig.configs.resolver.placeholder.Placeholder.isPlaceholder;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.warn;
import static java.util.regex.Matcher.quoteReplacement;

@RequiredArgsConstructor
class Template {
    private final File source;
    private final String text;

    Template(File source) {
        this(source, readFully(source));
    }

    String resolvePlaceholders(EnvComponent currentComponent, PropertyResolver propertyResolver, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return text;

        StringBuffer result = new StringBuffer();
        do {
            doResolve(m, result, propertyResolver, currentComponent);
        } while (m.find());
        m.appendTail(result);
        return result.toString();
    }

    private void doResolve(Matcher m, StringBuffer result, PropertyResolver propertyResolver, EnvComponent currentComponent) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String value = resolveValue(currentComponent, propertyResolver, m);
        m.appendReplacement(result, value == null ? "$0" : quoteReplacement(value));
    }

    private String resolveValue(EnvComponent currentComponent, PropertyResolver propertyResolver, Matcher matcher) {
        String placeholder = toPlaceholder(matcher);
        Property property = tempProperty("key", placeholder, currentComponent.getEnvironment(), specialSource(currentComponent.getComponent(), source.getAbsolutePath()));
        try {
            return propertyResolver.resolve(property, currentComponent);
        } catch (RuntimeException e) {
            warn("Template placeholder error: " + e.getMessage());
            return null;
        }
    }

    private String toPlaceholder(Matcher matcher) {
        String full = matcher.group();
        return isPlaceholder(full) ? full : "${this@" + full.substring("${".length());
    }
}