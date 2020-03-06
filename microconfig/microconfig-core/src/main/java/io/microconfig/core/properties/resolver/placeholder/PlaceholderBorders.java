package io.microconfig.core.properties.resolver.placeholder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

import static java.lang.Character.isLetterOrDigit;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

@With(PRIVATE)
@RequiredArgsConstructor
public class PlaceholderBorders {
    private static final PlaceholderBorders empty = new PlaceholderBorders(null);

    private final StringBuilder line;

    @Getter
    private final int startIndex;
    private final int envIndex;
    private final int valueIndex;
    private final int defaultValueIndex;
    @Getter
    private final int endIndex;

    private PlaceholderBorders(StringBuilder line) {
        this(line, -1, -1, -1, -1, -1);
    }

    public static PlaceholderBorders parse(StringBuilder line) {
        return new PlaceholderBorders(line).searchOpenSign();
    }

    private PlaceholderBorders searchOpenSign() {
        int index = line.indexOf("${", startIndex);
        if (index >= 0) {
            return new PlaceholderBorders(line)
                    .withStartIndex(index)
                    .parseComponentName();
        }

        return empty;
    }

    private PlaceholderBorders parseComponentName() {
        for (int i = startIndex + 2; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == '[') {
                return withEnvIndex(i + 1).parseEnvName();
            }
            if (c == '@') {
                return withValueIndex(i + 1).parseValue();
            }
            if (c == ':') {
                continue;
                //todo
            }
            if (!isAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return empty;
    }

    private PlaceholderBorders parseEnvName() {
        for (int i = envIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ']' && i + 1 < line.length() && line.charAt(i + 1) == '@') {
                return withValueIndex(i + 2).parseValue();
            }
            if (!isAllowedSymbol(c)) {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return empty;
    }

    private PlaceholderBorders parseValue() {
        for (int i = valueIndex; i < line.length(); ++i) {
            char c = line.charAt(i);
            if (c == ':') {
                return withDefaultValueIndex(i + 1).parseDefaultValue();
            }
            if (c == '}') {
                return withEndIndex(i);
            }
            if (!isAllowedSymbol(c) && c != '/' && c != '\\') {
                return withStartIndex(i).searchOpenSign();
            }
        }

        return empty;
    }

    private PlaceholderBorders parseDefaultValue() {
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
                return withEndIndex(i);
            }
        }

        return empty;
    }

    private boolean isAllowedSymbol(char c) {
        return isLetterOrDigit(c) || c == '.' || c == '_' || c == '-';
    }

    public Placeholder toPlaceholder(String contextEnv) {
//        return new Placeholder(
//                        empty(),
//                        line.subSequence(startIndex + 2, envIndex < 0 ? valueIndex - 1 : envIndex - 1).toString(),
//                        envIndex < 0 ? contextEnv : line.subSequence(envIndex, valueIndex - 2).toString(),
//                        line.subSequence(valueIndex, defaultValueIndex < 0 ? endIndex : defaultValueIndex - 1).toString(),
//                        ofNullable(defaultValueIndex < 0 ? null : line.subSequence(defaultValueIndex, endIndex).toString())
//                );

        return Placeholder.parse(line.substring(startIndex, endIndex + 1), contextEnv);
    }

    public boolean isValid() {
        return startIndex >= 0;
    }

    @Override
    public String toString() {
        return line.substring(startIndex, endIndex + 1);
    }
}