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

        StringBuilder sb = new StringBuilder();
        do {
            substituteProperty(properties, templatePattern, m, sb);
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private void substituteProperty(Map<String, String> properties, TemplatePattern templatePattern, Matcher m, StringBuilder sb) {
        if (m.group("escaped") != null) {
            m.appendReplacement(sb, quoteReplacement(m.group("placeholder")));
            return;
        }

        String value = resolve(properties, templatePattern, m.group("name"), m.group("defvalue"));
        m.appendReplacement(sb, value == null ? "$0" : quoteReplacement(value));
    }

    private String resolve(Map<String, String> properties, TemplatePattern templatePattern, String propertyName, String defaultValue) {
        String prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null) return prop;

        String sys = fromPrefix(propertyName, templatePattern.getSystemPropPrefix(), System::getProperty);
        if (sys != null) return sys;

        String env = fromPrefix(propertyName, templatePattern.getEnvPropPrefix(), System::getenv);
        if (env != null) return env;

        return null;
    }

    private String fromPrefix(String propertyName, String prefix, UnaryOperator<String> factory) {
        return propertyName.startsWith(prefix) ? factory.apply(propertyName.substring(prefix.length())) : null;
    }
}