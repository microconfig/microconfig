package io.microconfig.core.properties.resolver.placeholder;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

import static java.lang.Character.isLetterOrDigit;

@ToString
@RequiredArgsConstructor
public class PlaceholderBorders {
    private static final PlaceholderBorders empty = new PlaceholderBorders(null, -1, -1, -1, -1);

    private final String value;
    private final int startIndex;
    private final int valueIndex;
    private final int defaultValueIndex;
    private final int endIndex;

    public String placeholder() {
        return defaultValueIndex < 0 ? null : value.substring(startIndex, endIndex + 1);
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
            for (int i = currentIndex; i < value.length(); ++i) {
                char c = value.charAt(i);
                if (c == '[') {
                    return new ParsingEvnName().process(value, placeholderStart, i + 1);
                }
                if (c == '@') {
                    return new ParsingValue().process(value, placeholderStart, i + 1);
                }
                if (c == ':') {
                    continue;
                    //todo
                }
                if (!isAllowedSymbol(c)) {
                    return new SearchingOpenSign().process(value, currentIndex, currentIndex);
                }
            }

            return empty;
        }
    }

    private static class ParsingEvnName implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
            for (int i = currentIndex; i < value.length(); ++i) {
                char c = value.charAt(i);
                if (c == ']' && i + 1 < value.length() && value.charAt(i + 1) == '@') {
                    return new ParsingValue().process(value, placeholderStart, i + 2);
                }
                if (!isAllowedSymbol(c)) {
                    return new SearchingOpenSign().process(value, currentIndex, currentIndex);
                }
            }

            return empty;
        }
    }

    private static class ParsingValue implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
            for (int i = currentIndex; i < value.length(); ++i) {
                char c = value.charAt(i);
                if (c == ':') {
                    return new ParsingDefaultValue().process(value, placeholderStart, currentIndex + 1);
                }
                if (c == '}') {
                    return new PlaceholderBorders(value, placeholderStart, currentIndex, -1, i);
                }
                if (!isAllowedSymbol(c) && c != '/' && c != '\\') {
                    return new SearchingOpenSign().process(value, currentIndex, currentIndex);
                }
            }

            return empty;
        }

        private static class ParsingDefaultValue implements PlaceholderParserState {
            @Override
            public PlaceholderBorders process(String value, int placeholderStart, int currentIndex) {
                int openBrackets = 1;
                for (int i = currentIndex; i < value.length(); ++i) {
                    char c = value.charAt(i);
                    if (c == '{') {
                        char prevChar = value.charAt(i - 1);
                        if (prevChar == '$' || prevChar == '#') {
                            ++openBrackets;
                        }
                        continue;
                    }
                    if (c == '}' && --openBrackets == 0) {
                        return new PlaceholderBorders(value, placeholderStart, 0, currentIndex, i);
                    }
                }

                return empty;
            }
        }
    }

    private static boolean isAllowedSymbol(char c) {
        return isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }
}