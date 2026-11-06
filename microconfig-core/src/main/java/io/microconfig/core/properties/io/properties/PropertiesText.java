package io.microconfig.core.properties.io.properties;

import io.microconfig.core.properties.DeclaringComponent;
import io.microconfig.core.properties.FileBasedComponent;

import static io.microconfig.core.properties.io.properties.PropertiesSyntax.isSkippedBeforeValue;
import static io.microconfig.core.properties.io.properties.PropertiesSyntax.lineBreakEnd;

/**
 * Values of .properties files are kept as they are written there, with escapes and continuations,
 * other values (YAML, templates, expressions, env and system properties) are plain text.
 * Text crossing between them is converted, so that 'C:\temp' stays 'C:\temp' for the one who reads the result.
 */
public final class PropertiesText {
    private PropertiesText() {
    }

    public static boolean isPropertiesText(DeclaringComponent declaredIn) {
        return declaredIn instanceof FileBasedComponent && !((FileBasedComponent) declaredIn).isYaml();
    }

    // plain text to a .properties value: backslashes and line breaks would be read as escapes and continuations
    public static String escape(String plain) {
        StringBuilder result = new StringBuilder(plain.length());
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\n') {
                result.append("\\n");
            } else if (c == '\r') {
                result.append("\\r");
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // a .properties value to plain text, the way Properties.load reads it
    public static String unescape(String propertiesText) {
        StringBuilder result = new StringBuilder(propertiesText.length());
        for (int i = 0; i < propertiesText.length(); i++) {
            char c = propertiesText.charAt(i);
            if (c != '\\') {
                result.append(c);
            } else if (i + 1 < propertiesText.length()) {
                i = unescapeAt(propertiesText, i + 1, result);
            }
        }
        return result.toString();
    }

    // appends the char escaped at i and returns the last index it takes
    private static int unescapeAt(String text, int i, StringBuilder result) {
        int lineBreakEnd = lineBreakEnd(text, i);
        if (lineBreakEnd >= 0) return skipContinuationIndent(text, lineBreakEnd + 1) - 1;

        char c = text.charAt(i);
        if (c == 'u' && isUnicodeEscape(text, i + 1)) {
            result.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
            return i + 4;
        }
        result.append(c == 't' ? '\t' : c == 'n' ? '\n' : c == 'r' ? '\r' : c == 'f' ? '\f' : c);
        return i;
    }

    private static int skipContinuationIndent(String text, int i) {
        while (i < text.length() && isSkippedBeforeValue(text.charAt(i))) i++;
        return i;
    }

    private static boolean isUnicodeEscape(String text, int hexStart) {
        if (hexStart + 4 > text.length()) return false;
        for (int i = hexStart; i < hexStart + 4; i++) {
            if (Character.digit(text.charAt(i), 16) < 0) return false;
        }
        return true;
    }
}
