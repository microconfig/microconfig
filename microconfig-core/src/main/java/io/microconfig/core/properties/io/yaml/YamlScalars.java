package io.microconfig.core.properties.io.yaml;

import io.microconfig.core.properties.Property;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.microconfig.core.properties.io.properties.PropertiesText.unescape;
import static io.microconfig.core.properties.io.properties.PropertiesText.isPropertiesText;
import static io.microconfig.core.properties.ConfigFormat.PROPERTIES;
import static org.yaml.snakeyaml.DumperOptions.ScalarStyle.DOUBLE_QUOTED;

/**
 * Microconfig keeps YAML values as YAML fragments, so plain text written into YAML must be a valid fragment.
 */
public final class YamlScalars {
    private YamlScalars() {
    }

    // a .properties value written into YAML: '\#ff0000' becomes '"#ff0000"', not a comment
    public static String fromPropertiesText(String propertiesText) {
        return fromPlainText(unescape(propertiesText));
    }

    public static String fromPlainText(String text) {
        return readsBackAsSameText(text) ? text : doubleQuoted(text);
    }

    public static String stringValue(String text) {
        return readsBackAsString(text) ? text : doubleQuoted(text);
    }

    public static String literalValue(String text) {
        return text.indexOf('\n') >= 0 && !text.startsWith("-") ? text : stringValue(text);
    }

    public static String serializedValue(Property property) {
        if (isPropertiesText(property.getDeclaringComponent())) return fromPropertiesText(property.getValue());
        if (property.isLiteralText()) return literalValue(property.getValue());
        return property.getConfigFormat() == PROPERTIES ? fromPlainText(property.getValue()) : property.getValue();
    }

    public static boolean isCollection(String value) {
        return value.startsWith("-") || value.startsWith("[") || value.startsWith("{");
    }

    public static boolean isQuoted(String value) {
        if (value.length() < 2 || value.indexOf('\n') >= 0) return false;
        char quote = value.charAt(0);
        return (quote == '"' || quote == '\'') && value.charAt(value.length() - 1) == quote;
    }

    public static String unquote(String value) {
        return isQuoted(value) ? new Yaml().load(value) : value;
    }

    // numbers and booleans are read as the same text too, so they stay unquoted
    static boolean readsBackAsSameText(String text) {
        Object value = readBack(text);
        return value != NOT_A_SCALAR && value != null && text.equals(String.valueOf(value));
    }

    static boolean readsBackAsString(String text) {
        return text.equals(readBack(text));
    }

    public static String doubleQuoted(String text) {
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(DOUBLE_QUOTED);
        options.setSplitLines(false);
        return new Yaml(options).dump(text).trim();
    }

    private static final Object NOT_A_SCALAR = new Object();

    private static Object readBack(String text) {
        if (text.isEmpty() || text.contains("\n") || !text.equals(text.trim())) return NOT_A_SCALAR;
        try {
            Yaml yaml = new Yaml();
            Object value = yaml.<Map<?, ?>>load("value: " + text).get("value");
            if (value instanceof Map || value instanceof Iterable) return NOT_A_SCALAR;

            List<?> flow = yaml.load("[" + text + "]");
            return flow.size() == 1 && Objects.equals(value, flow.get(0)) ? value : NOT_A_SCALAR;
        } catch (YAMLException | ClassCastException e) {
            return NOT_A_SCALAR;
        }
    }
}
