package io.microconfig.core.properties.resolver.placeholder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static java.lang.Character.isLetterOrDigit;
import static java.util.Optional.empty;
import static java.util.Optional.of;

@RequiredArgsConstructor
public class PlaceholderBorders {
    private static final PlaceholderBorders empty = new PlaceholderBorders(null);

    private final StringBuilder line; //to char sequence

    @Getter
    private int startIndex = -1;
    private int envIndex;
    private int valueIndex;
    private int defaultValueIndex;
    @Getter
    private int endIndex;

    public static PlaceholderBorders parse(CharSequence line) {
        return parse(new StringBuilder(line));
    }

    public static PlaceholderBorders parse(StringBuilder line) {
        return new PlaceholderBorders(line).new SearchingOpenSign().process();
    }

    interface PlaceholderParserState {
        PlaceholderBorders process();
    }

    @RequiredArgsConstructor
    private class SearchingOpenSign implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process() {
            startIndex = line.indexOf("${", startIndex);
            reset();
            if (startIndex >= 0) {
                return new ParsingComponentName().process();
            }

            return empty;
        }

        private void reset() {
            envIndex = valueIndex = defaultValueIndex = endIndex = -1;
        }
    }

    private class ParsingComponentName implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process() {
            for (int i = startIndex + 2; i < line.length(); ++i) {
                char c = line.charAt(i);
                if (c == '[') {
                    envIndex = i + 1;
                    return new ParsingEvnName().process();
                }
                if (c == '@') {
                    valueIndex = i + 1;
                    return new ParsingValue().process();
                }
                if (c == ':') {
                    continue;
                    //todo
                }
                if (!isAllowedSymbol(c)) {
                    startIndex = i;
                    return new SearchingOpenSign().process();
                }
            }

            return empty;
        }
    }

    private class ParsingEvnName implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process() {
            for (int i = envIndex; i < line.length(); ++i) {
                char c = line.charAt(i);
                if (c == ']' && i + 1 < line.length() && line.charAt(i + 1) == '@') {
                    valueIndex = i + 2;
                    return new ParsingValue().process();
                }
                if (!isAllowedSymbol(c)) {
                    startIndex = i;
                    return new SearchingOpenSign().process();
                }
            }

            return empty;
        }
    }

    private class ParsingValue implements PlaceholderParserState {
        @Override
        public PlaceholderBorders process() {
            for (int i = valueIndex; i < line.length(); ++i) {
                char c = line.charAt(i);
                if (c == ':') {
                    defaultValueIndex = i + 1;
                    return new ParsingDefaultValue().process();
                }
                if (c == '}') {
                    endIndex = i;
                    return PlaceholderBorders.this;
                }
                if (!isAllowedSymbol(c) && c != '/' && c != '\\') {
                    startIndex = i;
                    return new SearchingOpenSign().process();
                }
            }

            return empty;
        }

        private class ParsingDefaultValue implements PlaceholderParserState {
            @Override
            public PlaceholderBorders process() {
                int openBrackets = 1;
                for (int i = defaultValueIndex; i < line.length(); ++i) {
                    char c = line.charAt(i);
                    if (c == '{') {
                        char prevChar = line.charAt(i - 1);
                        if (prevChar == '$' || prevChar == '#') {
                            ++openBrackets;
                        }
                        continue;
                    }
                    if (c == '}' && --openBrackets == 0) {
                        endIndex = i;
                        return PlaceholderBorders.this;
                    }
                }

                return empty;
            }
        }
    }

    private boolean isAllowedSymbol(char c) {
        return isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }

    public Optional<Placeholder> toPlaceholder(String contextEnv) {
//        return startIndex < 0 ?
//                empty() :
//                of(new Placeholder(
//                        empty(),
//                        line.subSequence(startIndex + 2, envIndex < 0 ? valueIndex - 1 : envIndex - 1).toString(),
//                        envIndex < 0 ? contextEnv : line.subSequence(envIndex, valueIndex - 2).toString(),
//                        line.subSequence(valueIndex, defaultValueIndex < 0 ? endIndex : defaultValueIndex - 1).toString(),
//                        ofNullable(defaultValueIndex < 0 ? null : line.subSequence(defaultValueIndex, endIndex).toString())
//                ));

        return startIndex < 0 ? empty() : of(Placeholder.parse(line.substring(startIndex, endIndex + 1), contextEnv));
    }
}