package io.microconfig.core.properties.io.properties;

/**
 * .properties rules shared by the reader and the writer, so that they can't disagree.
 */
public final class PropertiesSyntax {
    private PropertiesSyntax() {
    }

    // a line ending with an odd number of backslashes continues on the next line
    static boolean endsWithContinuation(CharSequence text) {
        int backslashes = 0;
        for (int i = text.length() - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            ++backslashes;
        }
        return backslashes % 2 == 1;
    }

    // whitespace that Properties.load skips before a value
    static boolean isSkippedBeforeValue(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    // plain text, e.g. a YAML value, inserted into a .properties value: its backslashes and line breaks would be read as escapes and continuations
    public static String escapeInsertedText(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '\r' || c == '\n') {
                result.append("\\n");
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
