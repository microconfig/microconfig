package deployment.mgmt.configs.updateconfigs.templates;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@RequiredArgsConstructor
public class Template {
    public static String PLACEHOLDER_PATTERN = "(?<escaped>\\\\)?"
            + "(?<placeholder>"
            + "\\$"
            + "\\{"
            + "(?<name>.+?)"
            + "(?:\\:(?<defvalue>.*?))??"
            + "})";
    private static final Pattern placeholderTemplate = compile(PLACEHOLDER_PATTERN);

    private static final String SYSPROP_PREFIX = "sysprop.";
    private static final String ENV_PREFIX = "env.";

    private final String text;

    public String resolvePlaceholders(Map<String, String> properties) {
        Matcher m = placeholderTemplate.matcher(text);
        if (!m.find()) return text;

        StringBuilder sb = new StringBuilder();
        do {
            substituteProperty(properties, m, sb);
        } while (m.find());
        m.appendTail(sb);
        return sb.toString();
    }

    private void substituteProperty(Map<String, String> properties, Matcher m, StringBuilder sb) {
        if (m.group("escaped") != null) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group("placeholder")));
            return;
        }

        String replacement = findReplacement(properties, m.group("name"), m.group("defvalue"));
        m.appendReplacement(sb, replacement == null ? "$0" : Matcher.quoteReplacement(replacement));
    }

    private String findReplacement(Map<String, String> properties, String propertyName, String defaultValue) {
        String prop = properties.getOrDefault(propertyName, defaultValue);
        if (prop != null) return prop;

        String sys = fromPrefix(propertyName, SYSPROP_PREFIX, System::getProperty);
        if (sys != null) return sys;

        String env = fromPrefix(propertyName, ENV_PREFIX, System::getenv);
        if (env != null) return env;

        return null;
    }

    private String fromPrefix(String propertyName, String prefix, UnaryOperator<String> factory) {
        return propertyName.startsWith(prefix) ? factory.apply(propertyName.substring(prefix.length())) : null;
    }
}