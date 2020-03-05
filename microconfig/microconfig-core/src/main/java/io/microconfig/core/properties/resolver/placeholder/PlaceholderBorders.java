package io.microconfig.core.properties.resolver.placeholder;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static java.lang.Character.isLetterOrDigit;

@ToString
@RequiredArgsConstructor
public class PlaceholderBorders {
    private static final PlaceholderBorders empty = new PlaceholderBorders(null, -1, -1);

    private final String value;
    private final int start;
    private final int end;

    public String placeholder() {
        return start < 0 ? null : value.substring(start, end);
    }

    public static PlaceholderBorders borders(String line) {
        return new SearchingOpenSign().process(line, 0, 0);
    }

    interface PlaceholderParserState {
        PlaceholderBorders process(String value, int placeholderStart, int currentIndex);
    }

    @RequiredArgsConstructor
    private static class SearchingOpenSign implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process(String value, int ignore, int currentIndex) {
            int startIndex = value.indexOf("${", currentIndex);
            if (startIndex < 0) {
                return empty;
            }
            return new ParsingComponentName().process(value, startIndex, startIndex + 2);
        }
    }

    private static class ParsingComponentName implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
            for (int i = currentIndex; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '@') {
                    return new ParsingValue().process(value, placeholderStart, i + 1);
                }
                if (!isLetterOrDigit(c) && c != ':' && c != '[' && c != ']' && c != '.' && c != '_' && c != '-') {
                    return new SearchingOpenSign().process(value, currentIndex + 1, currentIndex + 1);
                }
            }

            return empty;
        }
    }

    private static class ParsingValue implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
            for (int i = currentIndex; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == ':') {
                    return new ParsingDefaultValue().process(value, placeholderStart, currentIndex + 1);
                }
                if (c == '}') {
                    return new PlaceholderBorders(value, placeholderStart, i + 1);
                }
                if (!isLetterOrDigit(c) && c != '/' && c != '\\' && c != '.' && c != '_' && c != '-') {
                    return new SearchingOpenSign().process(value, currentIndex + 1, currentIndex + 1);
                }
            }
            return empty;
        }

        private static class ParsingDefaultValue implements PlaceholderParserState {
            @Override
            public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
                int openBrackets = 1;
                for (int i = currentIndex; i < value.length(); i++) {
                    char c = value.charAt(i);
                    if (c == '{') {
                        char prevChar = value.charAt(i - 1);
                        if (prevChar == '$' || prevChar == '#') {
                            ++openBrackets;
                        }
                        continue;
                    }
                    if (c == '}') {
                        if (--openBrackets == 0) {
                            return new PlaceholderBorders(value, placeholderStart, i + 1);
                        }
                    }
                }

                return empty;
            }
        }
    }
}