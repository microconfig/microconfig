package io.microconfig.core.properties.io.properties;

/**
 * .properties rules shared by the reader, the writer and PropertiesText, so that they can't disagree.
 */
final class PropertiesSyntax {
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

    // whitespace that Properties.load skips before a value and after a continuation
    static boolean isSkippedBeforeValue(char c) {
        return c == ' ' || c == '\t' || c == '\f';
    }

    // the last index of a line break starting at i ('\r\n', '\n' or '\r'), or -1
    static int lineBreakEnd(CharSequence text, int i) {
        char c = text.charAt(i);
        if (c == '\r') return i + 1 < text.length() && text.charAt(i + 1) == '\n' ? i + 1 : i;
        return c == '\n' ? i : -1;
    }
}
