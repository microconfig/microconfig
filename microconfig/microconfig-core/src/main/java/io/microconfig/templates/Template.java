package io.microconfig.templates;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;

import static java.util.regex.Matcher.quoteReplacement;

@RequiredArgsConstructor
public class Template {
    private final String text;

    public String resolvePlaceholders(Map<String, String> properties, TemplatePattern templatePattern) {
        Matcher m = templatePattern.getPattern().matcher(text);
        if (!m.find()) return text;

        StringBuilder result = new StringBuilder();
        do {
            doResolve(properties, templatePattern, m, result);
        } while (m.find());
        m.appendTail(result);
        return result.toString();
    }

    private void doResolve(Map<String, String> properties, TemplatePattern templatePattern, Matcher m, StringBuilder result) {
        if (m.group("escaped") != null) {
            m.appendReplacement(result, quoteReplacement(m.group("placeholder")));
            return;
        }

        String value = resolveValue(properties, m.group("name"), m.group("defvalue"), templatePattern);
        m.appendReplacement(result, value == null ? "$0" : quoteReplacement(value));
    }

    private String resolveValue(Map<String, String> properties, String key, String defaultValue, TemplatePattern templatePattern) {
        String prop = properties.getOrDefault(key, defaultValue);
        if (prop != null) return prop;

        String sys = fromPrefix(key, templatePattern.getSystemPropPrefix(), System::getProperty);
        if (sys != null) return sys;

        String env = fromPrefix(key, templatePattern.getEnvPropPrefix(), System::getenv);
        if (env != null) return env;

        return null;
    }

    private String fromPrefix(String propertyName, String prefix, UnaryOperator<String> factory) {
        return propertyName.startsWith(prefix) ? factory.apply(propertyName.substring(prefix.length())) : null;
    }
}