package io.microconfig.commands.buildconfigs.features.templates;

import io.microconfig.core.properties.Property;
import io.microconfig.core.properties.resolver.EnvComponent;
import io.microconfig.core.properties.resolver.PropertyResolver;
import io.microconfig.core.properties.resolver.placeholder.Placeholder;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.microconfig.core.properties.Property.tempProperty;
import static io.microconfig.core.properties.sources.SpecialSource.templateSource;
import static io.microconfig.utils.IoUtils.readFully;
import static io.microconfig.utils.Logger.warn;
import static io.microconfig.utils.StringUtils.addOffsets;
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

        String placeholder = m.group();
        String value = resolveValue(placeholder, currentComponent, propertyResolver);
        if (value == null) return;

        String finalValue = addOffsetForMultiLineValue(m, value);
        m.appendReplacement(result, quoteReplacement(finalValue));
    }

    private String addOffsetForMultiLineValue(Matcher m, String value) {
        int lineBeginIndex = text.lastIndexOf('\n', m.start());
        int placeholderOffset = m.start() - lineBeginIndex - 1;
        return value.replace("\n", addOffsets("\n", placeholderOffset));
    }

    private String resolveValue(String placeholder, EnvComponent currentComponent, PropertyResolver propertyResolver) {
        boolean microconfigFormatPlaceholder = isValidPlaceholder(placeholder);
        if (!microconfigFormatPlaceholder) {
            String newFormat = "${this@" + placeholder.substring("${".length());
            if (!isValidPlaceholder(newFormat)) return null;
            placeholder = newFormat;
        }

        try {
            return doResolve(currentComponent, propertyResolver, placeholder);
        } catch (RuntimeException e) {
            if (microconfigFormatPlaceholder) {
                warn("Template placeholder error: " + e.getMessage());
            }
            return null;
        }
    }

    public static boolean isValidPlaceholder(String value) {
        return Placeholder.parse(value).isValid();
    }

    private String doResolve(EnvComponent currentComponent, PropertyResolver propertyResolver, String placeholder) {
        Property property = tempProperty("key", placeholder, currentComponent.getEnvironment(), templateSource(currentComponent.getComponent(), source));
        return propertyResolver.resolve(property, currentComponent);
    }
}